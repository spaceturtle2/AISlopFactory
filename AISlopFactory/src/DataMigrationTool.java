// DataMigrationTool.java
import com.turtlestate.bank.model.User;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class DataMigrationTool {
    public static void main(String[] args) throws Exception {
        String input = "banking_data.dat";
        String output = "banking_data_shared.dat";
        File in = new File(input);
        if (!in.exists()) {
            System.err.println("Input file not found: " + input);
            return;
        }

        // read raw object
        Object raw;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(in))) {
            raw = ois.readObject();
        }

        if (!(raw instanceof Map)) {
            System.err.println("File did not contain a Map. Aborting.");
            return;
        }

        Map<?,?> rawMap = (Map<?,?>) raw;
        Map<String, User> rebuilt = new LinkedHashMap<>();

        for (Map.Entry<?,?> e : rawMap.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            if (!(k instanceof String)) continue;
            String unameKey = (String) k;

            if (v == null) {
                User u = new User(unameKey, "");
                rebuilt.put(unameKey, u);
                continue;
            }

            // If it's already our shared User, cast and copy
            if (v instanceof User) {
                User su = (User) v;
                rebuilt.put(su.getUsername(), su);
                continue;
            }

            // Otherwise attempt reflection to obtain properties
            String uname = null;
            String pass = "";
            double balance = 0.0;
            double loan = 0.0;
            Map<String,Integer> portfolio = null;

            Class<?> cls = v.getClass();

            try {
                Method m;
                m = safeGetMethod(cls, "getUsername");
                if (m != null) { Object r = m.invoke(v); if (r != null) uname = r.toString(); }
                m = safeGetMethod(cls, "getPassword");
                if (m != null) { Object r = m.invoke(v); if (r != null) pass = r.toString(); }
                m = safeGetMethod(cls, "getBalance");
                if (m != null) { Object r = m.invoke(v); if (r instanceof Number) balance = ((Number) r).doubleValue(); }
                m = safeGetMethod(cls, "getLoanBalance");
                if (m != null) { Object r = m.invoke(v); if (r instanceof Number) loan = ((Number) r).doubleValue(); }
                m = safeGetMethod(cls, "getPortfolio");
                if (m != null) {
                    Object r = m.invoke(v);
                    if (r instanceof Map) {
                        portfolio = new HashMap<>();
                        for (Object pe : ((Map) r).entrySet()) {
                            Map.Entry<?,?> ent = (Map.Entry<?,?>) pe;
                            Object pk = ent.getKey();
                            Object pv = ent.getValue();
                            if (pk != null && pv instanceof Number) {
                                portfolio.put(pk.toString(), ((Number) pv).intValue());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            // fallback fields
            if (uname == null) {
                try { Field f = cls.getDeclaredField("username"); f.setAccessible(true); Object ru = f.get(v); if (ru != null) uname = ru.toString(); } catch (Exception ignored) {}
            }
            try { Field f = cls.getDeclaredField("password"); f.setAccessible(true); Object rp = f.get(v); if (rp != null) pass = rp.toString(); } catch (Exception ignored) {}
            try { Field f = cls.getDeclaredField("balance"); f.setAccessible(true); Object rb = f.get(v); if (rb instanceof Number) balance = ((Number) rb).doubleValue(); } catch (Exception ignored) {}
            try { Field f = cls.getDeclaredField("loanBalance"); f.setAccessible(true); Object rl = f.get(v); if (rl instanceof Number) loan = ((Number) rl).doubleValue(); } catch (Exception ignored) {}
            try { Field f = cls.getDeclaredField("portfolio"); f.setAccessible(true); Object rp = f.get(v); if (rp instanceof Map) {
                    portfolio = new HashMap<>();
                    for (Object pe : ((Map) rp).entrySet()) {
                        Map.Entry<?,?> ent = (Map.Entry<?,?>) pe;
                        Object pk = ent.getKey();
                        Object pv = ent.getValue();
                        if (pk != null && pv instanceof Number) portfolio.put(pk.toString(), ((Number) pv).intValue());
                    }
                }
            } catch (Exception ignored) {}

            if (uname == null) uname = unameKey;
            User newU = new User(uname, pass);
            newU.setBalance(balance);
            newU.setLoanBalance(loan);
            if (portfolio != null) newU.setPortfolio(portfolio);
            rebuilt.put(newU.getUsername(), newU);
        }

        // write new file
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(output))) {
            oos.writeObject(rebuilt);
        }

        System.out.println("Migration complete. Wrote " + output + " with " + rebuilt.size() + " users.");
    }

    private static Method safeGetMethod(Class<?> cls, String name) {
        try { return cls.getMethod(name); } catch (NoSuchMethodException ex) { return null; }
    }
}
