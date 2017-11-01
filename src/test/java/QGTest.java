//import de.bonn.eis.controller.QuestionGenerator;
//import de.bonn.eis.model.SlideContent;
//import org.junit.Rule;
//import org.junit.Test;
//import org.mockito.Mock;
//import org.mockito.junit.MockitoJUnit;
//import org.mockito.junit.MockitoRule;
//
//import static org.mockito.Mockito.*;
//import static org.junit.Assert.*;
//
//import javax.servlet.ServletContext;
//import javax.ws.rs.core.Response;
//import java.io.FileNotFoundException;
//import java.io.UnsupportedEncodingException;
//
///**
// * Created by Ainuddin Faizan on 2/21/17.
// */
//public class QGTest {
//    @Mock
//    SlideContent contentMock;
//    ServletContext contextMock;
//    Response responseMock;
//    @Rule
//    public MockitoRule mockitoRule = MockitoJUnit.rule();
//
//    @Test
//    public void testQGen() {
//        QGenApplication questionGenerator = new QGenApplication();
////        when(questionGenerator.generateQuestionsForText(contentMock, contextMock)).thenReturn()
//        try {
//            if(questionGenerator == null){
//                System.out.println("Null object");
//            } else{
//                Response questions = questionGenerator.generateQuestionsForText(contentMock, contextMock);
//                assert (questions.getStatus() == 200);
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//    }
//}
