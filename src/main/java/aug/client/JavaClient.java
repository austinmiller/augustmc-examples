package aug.client;

import aug.script.framework.*;
import aug.script.framework.reload.Reload;
import aug.script.framework.reload.Reloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * <p>
 *     After compiling this in IntelliJ, the java tab under profile settings in August MC
 * is setup like so ...
 * </p>
 *
 * <ul>
 *     <li>autostart</li>
 *     <li>main class: aug.client.JavaClient</li>
 *     <li>classpath (add directory): ${project.basedir}/target/classes</li>
 * </ul>
 *
 */
@SuppressWarnings("unused")
public class JavaClient implements ClientInterface {

    @Reload
    private static long keepThisValue = 0;

    private ProfileInterface profile;

    private List<String> captureRegexes = new ArrayList<>(Arrays.asList(
            "^\\[ .* \\]: '.*'$",
            "^.* tells you, '.*'$",
            "^You tell .*, '.*'$",
            "^##.*",
            "^\\[\\*\\] .* (flame|flames) \\[\\*\\] '.*'$",
            "^.* says, '.*'$",
            "^< emote > .*$",
            "^< social > .*$",
            "^\\[ >>> .* <<< \\]$",
            "^Increased mastery in your ability of .*$"
    ));

    private String promptRegex = "H:\\[[0-9]+\\]";

    private TextWindowInterface com;
    private TextWindowInterface metric;

    private List<Class<?>> staticReloaders = new ArrayList<>(Arrays.asList(JavaClient.class));

    public void init(ProfileInterface profileInterface, ReloadData reloadData) {
        this.profile = profileInterface;

        com = profile.createTextWindow("com");
        metric = profile.createTextWindow("metric");

        SplitWindow graph = new SplitWindow(
                new WindowReference("console"),
                new SplitWindow(
                        new WindowReference("com"),
                        new WindowReference("metric"),
                        false, 0.8f
                ),
                true);
        profile.setWindowGraph(graph);

        // this line will set keepThisValue if a previous version of this client saved it.
        Reloader.loadStaticFields(reloadData, staticReloaders);
    }

    public ReloadData shutdown() {
        ReloadData rl = new ReloadData();
        Reloader.saveStaticFields(rl, staticReloaders);
        return rl;
    }

    public boolean handleLine(long lineNum, String raw) {
        String withoutColors = removeColors(raw);

        captureRegexes.stream().filter(withoutColors::matches).findFirst().ifPresent((s) -> {
            com.echo(raw);
        });

        return false;
    }

    public void handleFragment(String fragment) {
        if (fragment.matches(promptRegex)) {
            //do something
        } else if (fragment.matches("^password:$")) {
            profile.sendSilently("12345warpeace");
        }
    }

    public void handleGmcp(String s) {

    }

    private String helpMsg = "#help\n" +
            "You really need help.  Try some commands.\n" +
            "Or, read the manual.";

    private Pattern repeatCmd = Pattern.compile("^#([1-9]{1}[0-9]{0,}) (.*)$");

    public boolean handleCommand(String cmd) {
        if (cmd.matches("^#help$")) {
            for(String line : helpMsg.split("\n")) {
                metric.echo(line);
            }

            return true;
        }

        Matcher matcher = repeatCmd.matcher(cmd);
        if (matcher.matches()) {
            int count = Integer.parseInt(matcher.group(1));
            String cmdToSend = matcher.group(2);
            for (int i = 0; i < count; ++i) {
                profile.send(cmdToSend);
            }
        } else if (cmd.equals("increment")) {
            keepThisValue += 1;
            metric.echo("keepThisValue == " + keepThisValue);
        } else {
            for(String token : cmd.split(";")) {
                profile.send(token);
            }
        }

        // because we split on semicolon for non-matching commands and send to client,
        // we will never let the app send the command naturally.
        return true;
    }

    public void onConnect(long l, String s, int i) {

    }

    public void onDisconnect(long l) {

    }

    private String removeColors(String string) {
        return string.replaceAll("\u001B\\[.*?m", "");
    }
}
