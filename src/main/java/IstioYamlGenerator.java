import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import me.snowdrop.istio.api.model.v1.routing.DoneableRouteRule;
import me.snowdrop.istio.api.model.v1.routing.RouteRule;
import me.snowdrop.istio.api.model.v1.routing.RouteRuleBuilder;

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
                .build();
        OpenShiftClient client = new DefaultOpenShiftClient(config);

        // retrieve the custom resource definition for RouteRules
        final CustomResourceDefinition crd = client.customResourceDefinitions().withName("routerules.config.istio.io").get();

        // create a YAML mapper for YAML output
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        System.out.println("RouteRule CRD:");
        System.out.println(mapper.writeValueAsString(crd));

        // build a new RouteRule using fluent builder API
        RouteRule routeRule = new RouteRuleBuilder()
                .withNewMetadata()
                .withGenerateName("my-rule") // generate name automatically
                .endMetadata()
                .withNewDestination()
                .withName("greeting-service")
                .withNamespace("demo-istio") //optional
                .endDestination()
                .addNewRoute()
                .withWeight(100)
                .endRoute()
                .build();

        // create a new RouteRule resource
        final RouteRule generated = client.customResource(crd, RouteRule.class, KubernetesResourceList.class, DoneableRouteRule.class)
                .inNamespace("istio-system")
                .create(routeRule);

        System.out.println("Generated RouteRule:");
        System.out.println(mapper.writeValueAsString(generated));
    }
}
