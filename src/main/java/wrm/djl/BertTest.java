package wrm.djl;

import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.bert.BertTokenizer;
import ai.djl.modality.nlp.qa.QAInput;
import ai.djl.pytorch.zoo.nlp.qa.PtBertQATranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.Translator;
import java.nio.file.Paths;
import java.util.Map;

public class BertTest {

//  public static final String MODEL_PATH = "/Users/peter.muchaadevinta.com/Downloads/tf_model.h5";
  public static final String MODEL_PATH = "/Users/peter.muchaadevinta.com/Downloads/model.safetensors";

  public static void main(String[] args) throws Exception {

    Translator<QAInput, String> translator = new PtBertQATranslatorFactory().newInstance(
        QAInput.class, String.class, null, Map.of());
    Criteria<QAInput, String> criteria = Criteria.builder()
        .setTypes(QAInput.class, String.class)
        .optModelPath(Paths.get(MODEL_PATH))
        .optTranslator(translator)
        .optProgress(new ProgressBar()).build();

    ZooModel<QAInput, String> model = criteria.loadModel();
    Predictor<QAInput, String> predictor = model.newPredictor(translator);

    String question = "When did BBC Japan start broadcasting?";
    String paragraph =
        "BBC Japan was a general entertainment Channel. "
        + "Which operated between December 2004 and April 2006. "
        + "It ceased operations after its Japanese distributor folded.";
    QAInput input = new QAInput(question, paragraph);

    System.out.println(question);
    System.out.println(predictor.predict(input));
  }
}
