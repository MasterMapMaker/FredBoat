package fredboat.command.admin;

import fredboat.FredBoat;
import fredboat.commandmeta.abs.Command;
import fredboat.commandmeta.abs.ICommandOwnerRestricted;
import fredboat.util.ExitCodes;
import fredboat.util.log.SLF4JInputStreamErrorLogger;
import fredboat.util.log.SLF4JInputStreamLogger;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateCommand extends Command implements ICommandOwnerRestricted {

    private static final Logger log = LoggerFactory.getLogger(UpdateCommand.class);
    
    @Override
    public void onInvoke(Guild guild, TextChannel channel, User invoker, Message message, String[] args) {
        try {
            Runtime rt = Runtime.getRuntime();
            Message msg = channel.sendMessage("*Now updating...*\n\nRunning `git clone`... ");
            String branch = "master";
            if (args.length > 1) {
                branch = args[1];
            }

            //Clear any old update folder if it is still present
            try {
                Process rm = rt.exec("rm -rf update");
                rm.waitFor(5, TimeUnit.SECONDS);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Process gitClone = rt.exec("git clone https://github.com/Frederikam/FredBoat.git --branch " + branch + " --single-branch update");
            new SLF4JInputStreamLogger(log, gitClone.getInputStream()).start();
            new SLF4JInputStreamErrorLogger(log, gitClone.getInputStream()).start();
            
            if (gitClone.waitFor(120, TimeUnit.SECONDS) == false) {
                msg = msg.updateMessage(msg.getRawContent() + "[:anger: timed out]\n\n");
                throw new RuntimeException("Operation timed out: git clone");
            } else if (gitClone.exitValue() != 0) {
                msg = msg.updateMessage(msg.getRawContent() + "[:anger: returned code " + gitClone.exitValue() + "]\n\n");
                throw new RuntimeException("Bad response code");
            }

            msg = msg.updateMessage(msg.getRawContent() + "👌🏽\n\nRunning `mvn package shade:shade`... ");
            File updateDir = new File("./update");
            
            Process mvnBuild = rt.exec("mvn -f " + updateDir.getAbsolutePath() + "/pom.xml package shade:shade");
            new SLF4JInputStreamLogger(log, mvnBuild.getInputStream()).start();
            new SLF4JInputStreamErrorLogger(log, mvnBuild.getInputStream()).start();
            
            if (mvnBuild.waitFor(600, TimeUnit.SECONDS) == false) {
                msg = msg.updateMessage(msg.getRawContent() + "[:anger: timed out]\n\n");
                throw new RuntimeException("Operation timed out: mvn package shade:shade");
            } else if (mvnBuild.exitValue() != 0) {
                msg = msg.updateMessage(msg.getRawContent() + "[:anger: returned code " + mvnBuild.exitValue() + "]\n\n");
                throw new RuntimeException("Bad response code");
            }
            
            /*
            //Read and execute sh script
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("update.sh");
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            String source = "";
            while ((inputLine = in.readLine()) != null) {
                source = source + inputLine + "\n";
            }
            in.close();
            rt.exec(source);*/
            
            //Shutdown for update
            msg = msg.updateMessage(msg.getRawContent() + "👌🏽\n\nNow restarting...");
            FredBoat.shutdown(ExitCodes.EXIT_CODE_UPDATE);
        } catch (InterruptedException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
