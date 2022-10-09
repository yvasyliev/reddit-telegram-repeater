package app.funclub.anadearmas;

import app.funclub.anadearmas.config.AnadeArmasFunclubConfig;
import app.funclub.anadearmas.exceptions.UnhandledDataFormatException;
import app.funclub.anadearmas.telegram.AnadeArmasFanbot;
import com.github.masecla.RedditClient;
import com.github.masecla.config.ScriptClientConfig;
import com.github.masecla.objects.app.script.Credentials;
import com.github.masecla.objects.app.script.PersonalUseScript;
import com.github.masecla.objects.app.script.UserAgent;
import com.github.masecla.objects.reddit.Item;
import com.github.masecla.objects.reddit.Link;
import com.github.masecla.objects.reddit.Metadata;
import com.github.masecla.objects.reddit.Resolution;
import com.github.masecla.objects.reddit.Thing;
import com.github.masecla.objects.response.GetSubredditNewResponse;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

// channel id = -1001683714987
// user id = 390649240
public class Main {
    private static final String APP_CONFIG_PATH = "config.properties";
    private static final AnadeArmasFanbot ANA_DE_ARMAS_FANBOT = new AnadeArmasFanbot();
    private static final Properties PROPERTIES = new Properties();
    private static final RedditClient REDDIT_CLIENT = new RedditClient(
            new ScriptClientConfig(
                    new PersonalUseScript("pW134F0XNuueG4W78x9uGA", "gdsT7VkTgf2WLfSW7Pd6t5DQvfVueA"),
                    new UserAgent("Ana de Armas funclub", "1.0", "yvasyliev"),
                    new Credentials("this_is_literally_me", "DRC2epw*bej7ncf!anf")
            )
    );

    public static void main(String[] args) {
        try {
            AnadeArmasFunclubConfig config = new AnadeArmasFunclubConfig(APP_CONFIG_PATH);
            config.postConstruct();

            read(PROPERTIES);

            long created = Long.parseLong(PROPERTIES.getProperty("created", "0"));

            GetSubredditNewResponse subredditNew = REDDIT_CLIENT.getSubredditNew("AnadeArmas").rawJson().execute();
            List<Thing<Link>> children = subredditNew.getData().getChildren();
            Collections.reverse(children);

            for (Thing<Link> child : children) {
                Link link = child.getData();
                if (link.getCreated() > created) {
                    handlePost(link);

                    created = link.getCreated();
                    PROPERTIES.setProperty("created", String.valueOf(created));
                    ignoringException(() -> write(PROPERTIES));
                    sleep(10000);
                }
            }

        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
            ignoringException(() -> {
                try (StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter)) {
                    e.printStackTrace(printWriter);
                    ANA_DE_ARMAS_FANBOT.sendDeveloperMessage(stringWriter.toString());
                }
            });
        } catch (UnhandledDataFormatException e) {
            e.printStackTrace();
            ignoringException(() -> ANA_DE_ARMAS_FANBOT.sendDeveloperMessage(e.getMessage()));
        }
    }

    private static void handlePost(Link link) throws TelegramApiException, IOException, UnhandledDataFormatException {
        if (link.getUrlOverriddenByDest().endsWith(".gif")) {
            String gifUrl = link.getPreview()
                    .getImages()
                    .get(0)
                    .getVariants()
                    .getMp4()
                    .getSource()
                    .getUrl();

            ANA_DE_ARMAS_FANBOT.sendGif(gifUrl, link.getTitle());
        } else if (link.getGalleryData() != null) {
            List<String> photoUrls = link.getGalleryData()
                    .getItems()
                    .stream()
                    .map(Item::getMediaId)
                    .map(mediaId -> link.getMediaMetadata().get(mediaId))
                    .map(Metadata::getP)
                    .map(Collection::stream)
                    .map(stream -> stream.max(Comparator.comparingInt(Resolution::getWidth)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(Resolution::getUrl)
                    .collect(Collectors.toList());

            boolean manyPhotos = photoUrls.size() > 10;
            List<String> photoUrlsPage = manyPhotos ? photoUrls.subList(0, 10) : photoUrls;

            ANA_DE_ARMAS_FANBOT.sendMultiplePhotos(photoUrlsPage, link.getTitle());

            if (manyPhotos) {
                photoUrls.removeAll(photoUrlsPage);
                do {
                    sleep(10000);
                    int size = photoUrls.size();
                    int toIndex = Math.min(size, 10);
                    photoUrlsPage = photoUrls.subList(0, toIndex);
                    ANA_DE_ARMAS_FANBOT.sendMultiplePhotos(photoUrlsPage, null);
                    photoUrls.removeAll(photoUrlsPage);
                } while (!photoUrls.isEmpty());
            }
        } else if (link.getMedia() != null && link.getMedia().getRedditVideo() != null) {
            String videoUrl = link.getMedia().getRedditVideo().getFallbackUrl();
            ANA_DE_ARMAS_FANBOT.sendVideo(videoUrl, link.getTitle());
        } else if (link.getPreview() != null && link.getPreview().getRedditVideoPreview() != null && !"t2_dy21ymq9".equals(link.getAuthorFullname())) {
            String videoUrl = link.getPreview().getRedditVideoPreview().getFallbackUrl();
            ANA_DE_ARMAS_FANBOT.sendVideo(videoUrl, link.getTitle());
        } else if ("youtube.com".equals(link.getDomain()) || "youtu.be".equals(link.getDomain())) {
            String text = link.getTitle() + "\n\n" + link.getUrlOverriddenByDest();
            ANA_DE_ARMAS_FANBOT.sendText(text);
        } else if (link.getUrlOverriddenByDest().endsWith(".jpg1")) {
            String photoUrl = link.getUrlOverriddenByDest().substring(0, link.getUrlOverriddenByDest().length() - 1);
            ANA_DE_ARMAS_FANBOT.sendPhoto(photoUrl, link.getTitle());
        } else if (link.getUrlOverriddenByDest().endsWith(".jpg") || link.getUrlOverriddenByDest().endsWith(".png") || link.getUrlOverriddenByDest().endsWith("jpeg")) {
            String photoUrl = link.getPreview()
                    .getImages()
                    .get(0)
                    .getSource()
                    .getUrl();

            photoUrl = photoUrl.contains("auto=webp") ? link.getUrlOverriddenByDest() : photoUrl;

            try {
                ANA_DE_ARMAS_FANBOT.sendPhoto(photoUrl, link.getTitle());
            } catch (TelegramApiRequestException e) {
                if (!e.getApiResponse().contains("PHOTO_INVALID_DIMENSIONS") && !e.getApiResponse().endsWith("too big for a photo")) {
                    throw e;
                }
                ANA_DE_ARMAS_FANBOT.sendDocument(photoUrl, link.getTitle());
            }
        } else if (link.getCrosspostParentList() != null && !link.getCrosspostParentList().isEmpty()) {
            handlePost(link.getCrosspostParentList().get(0));
        } else if ("link".equals(link.getPostHint())) {
            ANA_DE_ARMAS_FANBOT.sendText(link.getTitle() + "\n" + link.getUrlOverriddenByDest());
        } else {
            throw new UnhandledDataFormatException("Could not handle post. Created: " + link.getCreated() + ", URL: " + link.getUrlOverriddenByDest());
        }
    }

    public static void read(Properties properties) throws IOException {
        File file = new File("config.properties");
        if (file.exists()) {
            try (InputStream inputStream = Files.newInputStream(file.toPath())) {
                properties.load(inputStream);
            }
        }
    }

    public static void write(Properties properties) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(Paths.get("config.properties"))) {
            properties.store(outputStream, null);
        }
    }

    public static void sleep(long millis) {
        ignoringException(() -> Thread.sleep(millis));
    }

    public static void ignoringException(CheckedExecutor checkedExecutor) {
        try {
            checkedExecutor.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
