import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.clnt.v3_1.Config;
import io.fabric8.kubernetes.clnt.v3_1.ConfigBuilder;
import io.fabric8.openshift.clnt.v3_1.DefaultOpenShiftClient;
import io.fabric8.openshift.clnt.v3_1.OpenShiftClient;
import me.snowdrop.istio.api.model.IstioResource;
import me.snowdrop.istio.api.model.IstioResourceBuilder;
import me.snowdrop.istio.client.IstioClient;
import me.snowdrop.istio.client.KubernetesAdapter;

public class IstioYamlGenerator {

    public static void main(String[] args) throws JsonProcessingException {
        if (args.length == 0 || args[0] == null) {
            throw new IllegalArgumentException("Must provide a valid host name or IP for the target OpenShift cluster as an argument to this application.");
        }

        final String clusterURL = "https://" + args[0] + ":8443";
        System.out.println("Connecting to OpenShift cluster at " + clusterURL + "\n");

        // create an OpenShift client to connect to the cluster
        Config config = new ConfigBuilder()
                .withMasterUrl(clusterURL)
                .withUsername("admin")
                .withPassword("admin")
                .withNamespace("istio-system")
                .build();
        OpenShiftClient client = new DefaultOpenShiftClient(config);

        KubernetesAdapter adapter = new KubernetesAdapter(client);
        IstioClient executor = new IstioClient(adapter);

        // build a new RouteRule using fluent builder API
        final IstioResource resource = new IstioResourceBuilder()
                .withNewMetadata()
                .withGenerateName("my-rule") // generate name automatically
                .endMetadata()
                .withNewRouteRuleSpec()
                .withNewDestination()
                .withName("greeting-service")
                .withNamespace("demo-istio") // optional
                .endDestination()
                .addNewRoute()
                .withWeight(100)
                .endRoute()
                .endRouteRuleSpec()
                .build();

        // create a new RouteRule resource
        final IstioResource generated = executor.registerCustomResource(resource);

        // create a YAML mapper for YAML output
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        System.out.println("Generated RouteRule:");
        System.out.println(mapper.writeValueAsString(generated));
    }
}
