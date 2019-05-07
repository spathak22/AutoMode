package automode.util;

public class Constants {
    public static final String EXAMPLES_SUFFIX = "";//"_train_pos";

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

    public enum ThresholdType {
        ABSOLUTE("abs"), PERCENTAGE("pctg");
        private String value;

        ThresholdType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Regex {
        SPLITON_OPEN_PARENTHESIS("\\("), CLOSE_PARENTHESIS(")"), OPEN_PARENTHESIS("("), PARENTHESIS(") < ("), PERIOD("."), SUBSET("<"), SEMICOLON(";"),
        COMMA(","), OPEN_BRACKET("["), CLOSE_BRACKET("]"), EMPTY_STRING("");
        private String value;

        Regex(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Voltdb{
        USERNAME("program"), PASSWORD("none"), URL("localhost"), PORT("21212");
        private String value;

        Voltdb(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum VoltdbNumber{
        SLEEP_TIME(1000), MAX_SLEEP_TIME(8000);
        private int value;

        VoltdbNumber(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum Inds{
        LEFT_RELATION("leftRelation"), LEFT_ATTRIBUTE_NUMBER("leftAttributeNumber"), RIGHT_RELATION("rightRelation"), RIGHT_ATTRIBUTE_NUMBER("rightAttributeNumber");
        private String value;

        Inds(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Modes{
        TARGET("target"), HEAD_MODE("headMode"), BODY_MODES("bodyModes"), SP_NAME("spName");
        private String value;

        Modes(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Log{
        OUTPUT_LOG_PATH("outputlog/automode.log");
        private String value;

        Log(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Types {
        TYPE_PREFIX("T"), EXACT_IND("Exact"), APPROX_IND("Approx");

        private String value;

        Types(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum HyperParameters{
        APPROX_TYPEASSIGN_THRESHOLD(0.1);
        private Double value;

        HyperParameters(Double value) {
            this.value = value;
        }

        public Double getValue() {
            return value;
        }
    }



}
