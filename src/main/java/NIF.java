import org.nlp2rdf.bean.NIFBean;
import org.nlp2rdf.bean.NIFType;
import org.nlp2rdf.nif21.impl.NIF21;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ainuddin Faizan on 12/6/16.
 */
public class NIF {
    public void nifRun(){
        NIFBean.NIFBeanBuilder contextBuilder = new NIFBean.NIFBeanBuilder();
        contextBuilder.context("http://freme-project.eu", 0, 33).mention("Diego Maradona is from Argentina.").nifType(NIFType.CONTEXT);
        NIFBean beanContext = new NIFBean(contextBuilder);

        NIFBean.NIFBeanBuilder entityBuilder = new NIFBean.NIFBeanBuilder();

        List<String> types = new ArrayList<String>();
        types.add("http://dbpedia.org/ontology/Place");
        types.add("http://dbpedia.org/ontology/Location");
        types.add("http://dbpedia.org/ontology/PopulatedPlace");
        types.add("http://nerd.eurecom.fr/ontology#Location");
        types.add("http://dbpedia.org/ontology/Country");
        types.add("http://dbpedia.org/ontology/Celebrity");

        entityBuilder.context("http://freme-project.eu", 23, 32).mention("Argentina").beginIndex(23).endIndex(32)
                .taIdentRef("http://dbpedia.org/resource/Argentina").score(0.9804963628413852)
                .annotator("http://freme-project.eu/tools/freme-ner")
                .types(types);

        NIFBean entityBean = new NIFBean(entityBuilder);

        List<NIFBean> beans = new ArrayList();
        beans.add(entityBean);
        beans.add(contextBuilder.build());

        NIF21 nif = new NIF21(beans);
        System.out.println(nif.getNTriples());
//        System.out.println(nif.getRDFxml());
//        System.out.println(beanContext.getReferenceContextId());
//        System.out.println(nif.getJSONLD(nif.getc));
    }
}
