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

    public AntBuilder(<ctx> input) {
        super(input);
        this.name = this.getClass().getSimpleName();
    }

    @Override
    public <In_ extends Serializable, Out_ extends Output, B_ extends Builder<In_, Out_>, F_ extends BuilderFactory<In_, Out_, B_>, SubIn_ extends In_> Out_ requireBuild(F_ factory, SubIn_ input) throws IOException {
        assert input instanceof <ctx>;
        return super.requireBuild(factory, (In_)((<ctx>)input).withName(name));
    }

    @Override
    protected String description(<ctx> context) {
        return "Builder "+name+": " + context;
    }

    @Override
    public File persistentPath(<ctx> context) {
        return new File("deps/"+name+".dep");
    }

    @Override
    protected Stamper defaultStamper() {
        return FileHashStamper.instance;
    }
}
