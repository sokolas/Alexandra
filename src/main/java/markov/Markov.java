package markov;

import org.springframework.beans.factory.annotation.Autowired;
import reactor.util.function.Tuple2;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Markov {
    public static final Pattern garbage = Pattern.compile("[^\\w,.:!?-]+", Pattern.UNICODE_CHARACTER_CLASS);
    public static final Pattern ending = Pattern.compile("[\\W]+$", Pattern.UNICODE_CHARACTER_CLASS);
    public static final Pattern url = Pattern.compile("(http|https)://[^\\s]+", Pattern.UNICODE_CHARACTER_CLASS);

    private static class Info {
        private Set<String> follow;
        private String word;
        private Set<String> stops;
        private boolean stop;
        private boolean start;

        Info() {
            follow = new HashSet<>();
            stops = new HashSet<>();
        }

        Info(String word) {
            this();
            this.word = word;
        }

        public Set<String> getFollow() {
            return follow;
        }

        public void setFollow(Set<String> follow) {
            this.follow = follow;
        }

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public Set<String> getStops() {
            return stops;
        }

        public void setStops(Set<String> stops) {
            this.stops = stops;
        }

        public boolean isStop() {
            return stop;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }

        public boolean isStart() {
            return start;
        }

        public void setStart(boolean start) {
            this.start = start;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("> ").append(word).append("\n");
            sb.append("start: ").append(start).append(", ")
                    .append("stop : ").append(stop)
                    .append("\n");
            sb.append("follow:\n\t")
                    .append(String.join("\n\t", follow))
                    .append("\n");
            sb.append("stops:\n\t")
                    .append(String.join("\n\t", stops))
                    .append("\n");
            return sb.toString();
        }
    }

    private Map<String, Info> data = new HashMap<>();
    private List<String> startWords = new ArrayList<>();
    private Random random = new Random();

    @Autowired
    private Consumer<Tuple2<IChannel, String>> sender;

    public void add(IMessage message) {
        String t = message.getContent();
        if (t == null || t.isEmpty())
            return;
        IUser author = message.getAuthor();
        String text1 = url.matcher(t).replaceAll(" ");
        String text = garbage.matcher(text1).replaceAll(" ");
        String[] words = text.split(" ");
        if (words == null || words.length == 0) {
            return;
        }
        String prevKey = "";
        for (String word:  words) {
            Matcher matcher = this.ending.matcher(word);
            String w = matcher.replaceAll("");
            matcher.reset();
            String key = w.toLowerCase();
            Info info;
            if (this.data.get(key) != null) {
                info = this.data.get(key);
            } else {
                info = new Info(w);
            }
            if (matcher.matches()) {
                String stop = matcher.group();
                info.setStop(true);
                info.getStops().add(stop);
            }
            matcher.reset();
            this.data.put(key, info);
            if (prevKey.isEmpty()) {
                info.setStart(true);
                this.startWords.add(w);
            } else {
                Info prevInfo = this.data.get(prevKey);
                prevInfo.getFollow().add(key);
            }
            prevKey = key;
        }
        if (!prevKey.isEmpty()) {
            this.data.get(prevKey).setStop(true);
            data.get(prevKey).getStops().add(".");
        }
    }

    public String generate() {
        int i = random.nextInt(startWords.size());
        String w = startWords.get(i).toLowerCase();
        StringBuilder sb = new StringBuilder(startWords.get(i));
        Info info = data.get(w);
        boolean finished = false;
        int size = 1;
        while (info.getFollow().size() > 0 && !finished && size < 30) {
            if (info.getStops().size() > 0) {
                if (random.nextDouble() < 0.4) {
                    ArrayList<String> stops = new ArrayList<>(info.getStops());
                    sb.append(stops.get(random.nextInt(stops.size())));
                } else {
                    if (info.isStop() && random.nextDouble() < 0.2) {
                        sb.append(".");
                        finished = true;
                    }
                }
            }
            ArrayList<String> follow = new ArrayList<>(info.getFollow());
            w = follow.get(random.nextInt(follow.size()));
            info = data.get(w);
            sb.append(" ").append(info.getWord());
            size++;
        }
        return sb.toString();
    }

    public String debug() {
        StringBuilder sb = new StringBuilder("```\n");
        for (Info value: data.values()) {
            sb.append(value.toString());
        }
        sb.append("```");
        return sb.toString();
    }
}
