import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.SystemARQ;
import org.apache.jena.vocabulary.VCARD;

/**
 * Created by andy on 12/6/16.
 */
public class JenaStuff {

    public void runJena() {
        // some definitions
        String personURI    = "http://somewhere/JohnSmith";
        String givenName = "John";
        String familyName = "Smith";
        String fullName     = givenName + " " + familyName;

        // create an empty Model
        Model model = ModelFactory.createDefaultModel();

        // create the resource
        Resource johnSmith = model.createResource(personURI)
                .addProperty(VCARD.FN, fullName)
                .addProperty(VCARD.N,
                        model.createResource()
                                .addProperty(VCARD.Given, givenName)
                                .addProperty(VCARD.Family, familyName));

        StmtIterator stmtIterator = johnSmith.getModel().listStatements();

        while(stmtIterator.hasNext()) {
            Statement statement = stmtIterator.nextStatement();
            Resource subject = statement.getSubject();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();

            System.out.print(subject.toString());
            System.out.print(" " + predicate.toString() + " ");

            if(object instanceof Resource)
                System.out.print(object.toString());
            else
                System.out.print("\"" + object.toString() + "\"");
            System.out.println(".");
        }

        model = johnSmith.getModel();

        model.write(System.out); // dumb writer (will also write blank nodes with a URI)
        model.write(System.out, "RDF/XML-ABBREV");
        model.write(System.out, "N-TRIPLES");
    }
}
