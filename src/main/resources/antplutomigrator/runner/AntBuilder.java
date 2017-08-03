package <pkg>;

import build.pluto.builder.Builder;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.output.Output;
import build.pluto.stamp.FileHashStamper;
import build.pluto.stamp.Stamper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public abstract class AntBuilder extends Builder<<ctx>, <ctx>> {

    private String name;
    private int hashCode;

    public AntBuilder(<ctx> context) {
        super(context);
        this.name = this.getClass().getSimpleName();
        this.hashCode = context.hashCode();
    }

    @Override
    public <In_ extends Serializable, Out_ extends Output, B_ extends Builder<In_, Out_>, F_ extends BuilderFactory<In_, Out_, B_>, SubIn_ extends In_> Out_ requireBuild(F_ factory, SubIn_ input) throws IOException {
        assert input instanceof <ctx>;
        return super.requireBuild(factory, (In_)((<ctx>)input).withName(name));
    }

    public <In_ extends <ctx>, Out_ extends <ctx>, B_ extends Builder<In_, Out_>, F_ extends BuilderFactory<In_, Out_, B_>, SubIn_ extends In_> void antCall(F_ factory,  SubIn_ context) {
        try {
            this.requireBuild(factory, context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String description(<ctx> context) {
        return "Builder "+name+": " + context;
    }

    @Override
    public File persistentPath(<ctx> context) {
        return new File("deps/"+name+"."+hashCode+".dep");
    }

    @Override
    protected Stamper defaultStamper() {
        return FileHashStamper.instance;
    }
}
