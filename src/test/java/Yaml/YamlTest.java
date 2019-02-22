package Yaml;

import io.github.sinri.passover.gateway.BasePassoverRouter;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class YamlTest {

    public static void main(String[] args) {
        try {
            InputStream input = new FileInputStream(new File("/Users/sinri/code/java/passover/config/passover.yml"));
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(input);

            map.forEach((key, value) -> {
                System.out.println("> " + key + " : " + value);
            });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Class<? extends BasePassoverRouter> subclass = Class.forName("io.github.sinri.passover.sample.Router.FirstRouter").asSubclass(BasePassoverRouter.class);
            BasePassoverRouter router = subclass.getDeclaredConstructor().newInstance();
            System.out.println("router name is " + router.name());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
