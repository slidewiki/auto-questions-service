public class Main {

    public static void main(String[] args) {
//        try {
////            new NLPStuff().runNLP();
//            new NLPStuff().runFrequencyNLP();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        DBPediaSpotlightClient dbPediaSpotlightClient = new DBPediaSpotlightClient();
        dbPediaSpotlightClient.init();
        dbPediaSpotlightClient.annotatePost(NLPConsts.article);
//        new JenaStuff().runJena();
//        new NIF().nifRun();
    }
}
