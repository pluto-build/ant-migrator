package <pkg>;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.sugarj.common.Log;

public class PlutoBuildListener implements BuildListener {
    @Override
    public void buildStarted(BuildEvent event) {

    }

    @Override
    public void buildFinished(BuildEvent event) {

    }

    @Override
    public void targetStarted(BuildEvent event) {

    }

    @Override
    public void targetFinished(BuildEvent event) {

    }

    @Override
    public void taskStarted(BuildEvent event) {

    }

    @Override
    public void taskFinished(BuildEvent event) {

    }

    @Override
    public void messageLogged(BuildEvent event) {
        // TODO: Might rework this to adapt internal logging priority to let pluto pick the detail of logging...
        if (event.getPriority() <= Project.MSG_WARN)
            Log.log.log(event.getMessage(), Log.ALWAYS);
    }
}
