package modetransform.util;

/**
 * Created by Sudhanshu on 27/08/17.
 */
public class Constants {

    public enum ModeType {
        INPUT("+"), OUTPUT("-"), CONSTANT("#");
        private String value;

        ModeType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum TransformDelimeter{
        ARROW("->"), SLASH_CLOSE_PARA("\\)"), CLOSE_PARA(")"), OPEN_PARA("("), COMMA(",");
        private String value;

        TransformDelimeter(String value){
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum FileName{
        INPUT_MODE_FILE("datamodel-input.json"),
        OUTPUT_MODE_FILE("datamodel-output.json"),
        TRANFORMATION_FILE("transform.txt");

        private String value;

        FileName(String value){
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
