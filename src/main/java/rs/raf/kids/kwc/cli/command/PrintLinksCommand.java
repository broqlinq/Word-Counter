package rs.raf.kids.kwc.cli.command;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class PrintLinksCommand implements Command {

    @Override
    public String getName() {
        return "links";
    }

    @Override
    public void execute(String... args) {
        if (args.length != 1)
            throw new IllegalArgumentException('\'' + getName() + "' command takes 1 argument, but " + args.length + " were passed");

        String url = args[0];
        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a");
            links.stream()
                    .map(link -> link.attr("abs:href"))
                    .toList()
                    .forEach(System.out::println);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
