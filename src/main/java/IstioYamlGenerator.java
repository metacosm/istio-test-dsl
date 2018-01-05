import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import me.snowdrop.istio.api.model.v1.routing.DestinationWeight;
import me.snowdrop.istio.api.model.v1.routing.DestinationWeightBuilder;
import me.snowdrop.istio.api.model.v1.routing.DoneableRouteRule;
import me.snowdrop.istio.api.model.v1.routing.RouteRule;
import me.snowdrop.istio.api.model.v1.routing.RouteRuleBuilder;

public class IstioYamlGenerator {

    private static final String CLUSTER = "https://192.168.64.7:8443";

    public static void main(String[] args) throws JsonProcessingException {

        Config config = new ConfigBuilder()
                .withMasterUrl(CLUSTER)
                .withUsername("admin")
                .withPassword("admin")
                .build();
        OpenShiftClient client = new DefaultOpenShiftClient(config);

        final CustomResourceDefinition crd = client.customResourceDefinitions().withName("routerules.config.istio.io").get();
        System.out.println("crd = " + crd);


        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        // for(Service item : istioServices.getItems()){
        //     System.out.println(mapper.writeValueAsString(item));
        // };

        DestinationWeight destinationWeight = new DestinationWeightBuilder()
                .withWeight(100).build();

        RouteRule routeRule = new RouteRuleBuilder()
                .withNewMetadata()
                .withGenerateName("my-rule")
                .endMetadata()
                .withNewDestination()
                .withName("greeting-service")
                .withNamespace("demo-istio") //optional
                .endDestination()
                .withRoute(destinationWeight)
                .build();


        final RouteRule generated = client.customResource(crd, RouteRule.class, KubernetesResourceList.class, DoneableRouteRule.class)
                .inNamespace("istio-system")
                .create(routeRule);

        System.out.println("Generated Rule : " + mapper.writeValueAsString(generated));
    }
}
