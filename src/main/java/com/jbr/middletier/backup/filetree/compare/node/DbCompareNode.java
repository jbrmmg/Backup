package com.jbr.middletier.backup.filetree.compare.node;

import com.jbr.middletier.backup.filetree.FileTreeNode;
import com.jbr.middletier.backup.filetree.database.DbFile;
import com.jbr.middletier.backup.filetree.database.DbNode;

import java.util.*;

public class DbCompareNode  extends FileTreeNode {
    public enum ActionType { NONE, COPY, REMOVE, RECREATE_AS_FILE, RECREATE_AS_DIRECTORY }
    public enum SubActionType { NONE, WARN, IGNORE, REMOVE_SOURCE, DATE_UPDATE }

    private final ActionType actionType;
    private final SubActionType subActionType;
    private final boolean isDirectory;
    private final DbNode source;
    private final DbNode destination;

    static class ActionDecision {
        private final ActionDecisionItem decision;
        private static final Map<String,ActionDecisionItem> decisionMap;
        static {
            decisionMap = new HashMap<>();
            decisionMap.put("FILE-FILE-EQUAL-WARN", new ActionDecisionItem(ActionType.REMOVE, SubActionType.WARN));
            decisionMap.put("FILE-FILE-EQUAL-IGNORE", new ActionDecisionItem(ActionType.REMOVE, SubActionType.IGNORE));
            decisionMap.put("FILE-FILE-EQUAL-DELETE", new ActionDecisionItem(ActionType.REMOVE, SubActionType.REMOVE_SOURCE));
            decisionMap.put("FILE-FILE-EQUAL-OTHER", new ActionDecisionItem(ActionType.NONE, SubActionType.NONE));
            decisionMap.put("FILE-FILE-EQUAL_EXCEPT_DATE-WARN", new ActionDecisionItem(ActionType.REMOVE, SubActionType.WARN));
            decisionMap.put("FILE-FILE-EQUAL_EXCEPT_DATE-IGNORE", new ActionDecisionItem(ActionType.REMOVE, SubActionType.IGNORE));
            decisionMap.put("FILE-FILE-EQUAL_EXCEPT_DATE-DELETE", new ActionDecisionItem(ActionType.REMOVE, SubActionType.REMOVE_SOURCE));
            decisionMap.put("FILE-FILE-EQUAL_EXCEPT_DATE-OTHER", new ActionDecisionItem(ActionType.COPY, SubActionType.DATE_UPDATE));
            decisionMap.put("FILE-FILE-NOT_EQUAL-WARN", new ActionDecisionItem(ActionType.REMOVE, SubActionType.WARN));
            decisionMap.put("FILE-FILE-NOT_EQUAL-IGNORE", new ActionDecisionItem(ActionType.REMOVE, SubActionType.IGNORE));
            decisionMap.put("FILE-FILE-NOT_EQUAL-DELETE", new ActionDecisionItem(ActionType.REMOVE, SubActionType.REMOVE_SOURCE));
            decisionMap.put("FILE-FILE-NOT_EQUAL-OTHER", new ActionDecisionItem(ActionType.COPY, SubActionType.NONE));
            decisionMap.put("FILE-DIRECTORY-NOT_EQUAL-WARN", new ActionDecisionItem(ActionType.REMOVE, SubActionType.WARN));
            decisionMap.put("FILE-DIRECTORY-NOT_EQUAL-IGNORE", new ActionDecisionItem(ActionType.REMOVE, SubActionType.IGNORE));
            decisionMap.put("FILE-DIRECTORY-NOT_EQUAL-DELETE", new ActionDecisionItem(ActionType.REMOVE, SubActionType.REMOVE_SOURCE));
            decisionMap.put("FILE-DIRECTORY-NOT_EQUAL-OTHER", new ActionDecisionItem(ActionType.RECREATE_AS_FILE, SubActionType.NONE));
            decisionMap.put("FILE-MISSING-NOT_EQUAL-WARN", new ActionDecisionItem(ActionType.COPY, SubActionType.WARN));
            decisionMap.put("FILE-MISSING-NOT_EQUAL-IGNORE", new ActionDecisionItem(ActionType.COPY, SubActionType.IGNORE));
            decisionMap.put("FILE-MISSING-NOT_EQUAL-DELETE", new ActionDecisionItem(ActionType.COPY, SubActionType.REMOVE_SOURCE));
            decisionMap.put("FILE-MISSING-NOT_EQUAL-OTHER", new ActionDecisionItem(ActionType.COPY, SubActionType.NONE));
            decisionMap.put("DIRECTORY-DIRECTORY-EQUAL-OTHER", new ActionDecisionItem(ActionType.NONE, SubActionType.NONE));
            decisionMap.put("DIRECTORY-MISSING-NOT_EQUAL-OTHER", new ActionDecisionItem(ActionType.COPY, SubActionType.NONE));
            decisionMap.put("DIRECTORY-FILE-NOT_EQUAL-OTHER", new ActionDecisionItem(ActionType.RECREATE_AS_DIRECTORY, SubActionType.NONE));
            decisionMap.put("MISSING-FILE-NOT_EQUAL-OTHER", new ActionDecisionItem(ActionType.REMOVE, SubActionType.NONE));
            decisionMap.put("MISSING-DIRECTORY-NOT_EQUAL-OTHER", new ActionDecisionItem(ActionType.REMOVE, SubActionType.NONE));
        }

        private static class ActionDecisionItem {
            private final ActionType action;
            private final SubActionType subAction;

            public ActionDecisionItem(ActionType action, SubActionType subAction ) {
                this.action = action;
                this.subAction = subAction;
            }
        }

        private static String getSourceKey(DbNode node) {
            if(node != null) {
                if(node.isDirectory()) {
                    return "DIRECTORY";
                }

                return "FILE";
            }

            return "MISSING";
        }

        private static String getCompareKey(DbNode source, DbNode destination) {
            if(source != null && destination != null) {
                switch(source.compare(destination)) {
                    case DBC_EQUAL:
                        return "EQUAL";
                    case DBC_EQUAL_EXCEPT_DATE:
                        return "EQUAL_EXCEPT_DATE";
                    default:
                        // Nothing further, continue.
                }
            }

            return "NOT_EQUAL";
        }

        private static String getClassificationKey(DbNode source) {
            if((source instanceof DbFile file) &&  (file.getClassification() != null)) {
                switch (file.getClassification().getAction()) {
                    case CA_WARN:
                        return "WARN";
                    case CA_IGNORE:
                        return "IGNORE";
                    case CA_DELETE:
                        return "DELETE";
                    default:
                        // Nothing further, continue.
                }
            }

            return "OTHER";
        }

        private static String getKey(DbNode source, DbNode destination) {
            return getSourceKey(source) + "-" +
                    getSourceKey(destination) + "-" +
                    getCompareKey(source,destination) + "-" +
                    getClassificationKey(source);
        }

        public ActionDecision(DbNode source, DbNode destination) {
            // Find the required action.
            this.decision = decisionMap.get(getKey(source,destination));
        }

        public ActionType getAction() {
            return this.decision.action;
        }

        public SubActionType getSubAction() {
            return this.decision.subAction;
        }
    }

    public DbCompareNode(FileTreeNode parent, DbNode source, DbNode destination) {
        super(parent);

        ActionDecision actionDecision = new ActionDecision(source,destination);

        this.isDirectory = source.isDirectory();
        this.actionType = actionDecision.getAction();
        this.subActionType = actionDecision.getSubAction();
        this.source = source;
        this.destination = destination;
    }

    public DbCompareNode(FileTreeNode parent, boolean source, DbNode sourceOrDestination) {
        super(parent);
        this.isDirectory = sourceOrDestination.isDirectory();
        if(source) {
            ActionDecision actionDecision = new ActionDecision(sourceOrDestination,null);

            this.actionType = actionDecision.getAction();
            this.subActionType = actionDecision.getSubAction();
            this.source = sourceOrDestination;
            this.destination = null;
        } else {
            ActionDecision actionDecision = new ActionDecision(null, sourceOrDestination);

            this.actionType = actionDecision.getAction();
            this.subActionType = actionDecision.getSubAction();
            this.source = null;
            this.destination = sourceOrDestination;
        }
    }

    public ActionType getActionType() {
        return this.actionType;
    }

    public SubActionType getSubActionType() {
        return this.subActionType;
    }

    public boolean isDirectory() {
        return this.isDirectory;
    }

    public DbNode getSource() {
        return this.source;
    }

    public DbNode getDestination() {
        return this.destination;
    }

    @Override
    public Optional<String> getName() {
        return Optional.empty();
    }

    @Override
    protected void childAdded(FileTreeNode newChild) {
        // Nothing required.
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(this.actionType);
        result.append(" ");
        result.append(this.subActionType);

        if(this.source != null) {
            result.append(" ");
            result.append(this.source.getFSO().getIdAndType());
        }

        if(this.destination != null) {
            result.append(" ");
            result.append(this.destination.getFSO().getIdAndType());
        }

        return result.toString();
    }
}
