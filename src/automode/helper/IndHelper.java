package automode.helper;

import java.util.List;
import java.util.Map;

public class IndHelper {

    private String inds;
    private Map<String, List<String>> dbRelations;

    public Map<String, List<String>> getDbRelations() {
        return dbRelations;
    }

    public void setDbRelations(Map<String, List<String>> dbRelations) {
        this.dbRelations = dbRelations;
    }

    public String getInds() {
        return inds;
    }

    public void setInds(String inds) {
        this.inds = inds;
    }


}
