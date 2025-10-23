import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomWebsiteOpener {
    public static void main(String[] args) {
        try {
            // Read from websites.txt in the same package
            InputStream inputStream = RandomWebsiteOpener.class.getResourceAsStream("websites.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            List<String> urls = new ArrayList<>();
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    urls.add(line);
                }
            }
            reader.close();
            
            // Select and open random URL
            Random random = new Random();
            String randomUrl = urls.get(random.nextInt(urls.size()));
            Desktop.getDesktop().browse(new URI(randomUrl));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}