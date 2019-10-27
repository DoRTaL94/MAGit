//package components.commit.tree;
//
//import com.fxgraph.cells.AbstractCell;
//import com.fxgraph.graph.Graph;
//import com.fxgraph.graph.IEdge;
//import components.filetree.commit.CommitFileTree;
//import components.themes.ThemesController;
//import data.structures.Branch;
//import data.structures.Commit;
//import javafx.beans.binding.DoubleBinding;
//import javafx.beans.property.DoubleProperty;
//import javafx.beans.property.SimpleDoubleProperty;
//import javafx.geometry.Pos;
//import javafx.scene.layout.Pane;
//import javafx.scene.layout.Region;
//import javafx.scene.shape.Rectangle;
//import javafx.scene.text.Font;
//import javafx.scene.text.FontWeight;
//import javafx.scene.text.Text;
//import magit.CommitNode;
//import magit.Engine;
//
//import java.awt.Point;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.List;
//
//public class CommitGraphNode extends AbstractCell implements Comparable {
//    private static final String darkTheme = "resources/TreeNodeDark.css";
//    private static final String colorfulTheme = "resources/TreeNodeColorful.css";
//    private static final String defaultTheme = "resources/TreeNodeDefault.css";
//
//    public DoubleProperty CommitDetailsXProperty;
//    public DoubleProperty TreeNodeYProperty;
//    private HBoxCommitDetails commitDetails;
//    private Rectangle rectangleTreeNode;
//    private Pane paneRectangleTreeNode;
//    private CommitNode commitNode;
//    private List<CommitGraphNode> graphNodeParents;
//    private List<CommitGraphNode> graphNodeChildrens;
//    private int branchNumber;
//    private int idInList;
//    private String currentTheme;
//
//    public CommitGraphNode(CommitNode i_Node) {
//        commitNode = i_Node;
//        branchNumber = -1;
//        graphNodeChildrens = new ArrayList<>();
//        graphNodeParents = new ArrayList<>();
//        commitDetails = new HBoxCommitDetails();
//        TreeNodeYProperty      = new SimpleDoubleProperty(0);
//        CommitDetailsXProperty = new SimpleDoubleProperty(20);
//
//        commitDetails.setDoubleClickAction(() -> new CommitFileTree(commitNode));
//
//        if(ThemesController.themeChangedProperty.get().equals("Dark")) {
//            currentTheme = darkTheme;
//        } else if(ThemesController.themeChangedProperty.get().equals("Colorful")) {
//            currentTheme = colorfulTheme;
//        } else {
//            currentTheme = defaultTheme;
//        }
//
//        initializeRectangleTreeNode();
//        initializeCommitDetails();
//
//        paneRectangleTreeNode.getStylesheets().add(getClass().getResource(currentTheme).toExternalForm());
//        paneRectangleTreeNode.applyCss();
//
//        ThemesController.themeChangedProperty.addListener((observable, oldValue, newValue) -> {
//            if(newValue.equals("Dark")) {
//                currentTheme = darkTheme;
//            }
//            else if(newValue.equals("Colorful")) {
//                currentTheme = colorfulTheme;
//            }
//            else {
//                currentTheme = defaultTheme;
//            }
//
//            paneRectangleTreeNode.getStylesheets().clear();
//            paneRectangleTreeNode.getStylesheets().add(getClass().getResource(currentTheme).toExternalForm());
//            paneRectangleTreeNode.applyCss();
//        });
//    }
//
//
//    private double getTextWidth(String i_Text) {
//        Text textLabelBranch = new Text(i_Text);
//        textLabelBranch.setFont(Font.font("System", FontWeight.BOLD, 15));
//
//        return textLabelBranch.getLayoutBounds().getWidth();
//    }
//
//    private void initializeCommitDetails() {
//        if(this.getPointingBranches().size() != 0) {
//            for(Branch branch: this.getPointingBranches()) {
//                commitDetails.addBranchNameRectangle(branch);
//            }
//        }
//
//        double labelCommitDaysAgoWidth = getTextWidth(this.getCommit().getLastUpdate());
//        double labelCommitSha1Width = getTextWidth(this.getSha1());
//        double labelUserNameWidth = getTextWidth(this.getCommit().getLastChanger());
//        double labelPointedCommitDescriptionWidth = getTextWidth(this.getCommit().getMessage());
//
//        commitDetails.getLabelCommitDaysAgo().setPrefWidth(labelCommitDaysAgoWidth);
//        commitDetails.getLabelCommitDaysAgo().setText(this.getCommit().getLastUpdate());
//        commitDetails.getLabelCommitDaysAgo().setAlignment(Pos.CENTER);
//
//        commitDetails.getLabelCommitSha1().setPrefWidth(labelCommitSha1Width);
//        commitDetails.getLabelCommitSha1().setText(this.getSha1());
//        commitDetails.getLabelCommitSha1().setAlignment(Pos.CENTER);
//
//        commitDetails.getLabelUserName().setPrefWidth(labelUserNameWidth);
//        commitDetails.getLabelUserName().setText(this.getCommit().getLastChanger());
//        commitDetails.getLabelUserName().setAlignment(Pos.CENTER);
//
//        commitDetails.getLabelPointedCommitDescription().setPrefWidth(labelPointedCommitDescriptionWidth);
//        commitDetails.getLabelPointedCommitDescription().setText(this.getCommit().getMessage());
//        commitDetails.getLabelPointedCommitDescription().setAlignment(Pos.CENTER);
//
//        commitDetails.update();
//
//        commitDetails.getPaneSpacer().prefWidthProperty().bind(CommitDetailsXProperty);
//        commitDetails.getPaneSpacer().minWidthProperty().bind(CommitDetailsXProperty);
//        TreeNodeYProperty.bind(commitDetails.layoutYProperty());
//    }
//
//    private void initializeRectangleTreeNode() {
//        rectangleTreeNode = new Rectangle();
//        rectangleTreeNode.setWidth(16);
//        rectangleTreeNode.setHeight(16);
//        rectangleTreeNode.setId("rectangleTreeNode");
//
//        paneRectangleTreeNode = new Pane();
//        paneRectangleTreeNode.getChildren().add(rectangleTreeNode);
//        paneRectangleTreeNode.setPrefWidth(16);
//        paneRectangleTreeNode.setPrefHeight(16);
//
//        if(!currentTheme.isEmpty()) {
//            paneRectangleTreeNode.getStylesheets().clear();
//            paneRectangleTreeNode.getStylesheets().add(getClass().getResource(currentTheme).toExternalForm());
//            paneRectangleTreeNode.applyCss();
//        }
//    }
//
//    public List<CommitGraphNode> getGraphNodeParents() { return graphNodeParents; }
//
//    public void AddGraphNodeParent(CommitGraphNode i_Parent) {
//        graphNodeParents.add(i_Parent);
//        graphNodeParents.sort(CommitGraphNode::compareTo);
//    }
//
//    public int getBranchNumber() { return branchNumber; }
//
//    public void setBranchNumber(int i_Num) {
//        branchNumber = i_Num;
//    }
//
//    public List<CommitGraphNode> getGraphNodeChildren() { return graphNodeChildrens; }
//
//    public void AddGraphNodeChild(CommitGraphNode i_GraphNode) {
//        graphNodeChildrens.add(i_GraphNode);
//        graphNodeChildrens.sort(CommitGraphNode::compareTo);
//    }
//
//    public CommitNode getFirstParent() { return commitNode.getSecondParent(); }
//
//    public List<CommitNode> getChildren() { return commitNode.getChildren(); }
//
//    public List<Branch> getPointingBranches() { return commitNode.getPointingBranches(); }
//
//    public String getSha1() { return commitNode.getSha1(); }
//
//    public CommitNode getCommitNode() { return commitNode; }
//
//    public Commit getCommit() { return commitNode.getCommit(); }
//
//    public CommitNode getSecondParent() {return commitNode.getSecondParent(); }
//
//    @Override public Region getGraphic(Graph graph) {
//        return paneRectangleTreeNode;
//    }
//
//    @Override public DoubleBinding getXAnchor(Graph graph, IEdge edge) {
//        final Region graphic = graph.getGraphic(this);
//        return graphic.layoutXProperty().add(rectangleTreeNode.getWidth() / 2);
//    }
//
//    @Override public int compareTo(Object i_GraphNode) {
//        CommitGraphNode nodeToCompare = (CommitGraphNode) i_GraphNode;
//        long nodeToCompareTime = 0;
//        long thisNodeTime = 0;
//
//        try {
//            String nodeToCompareDate = nodeToCompare.getCommit().getLastUpdate();
//            String thisNodeDate = getCommit().getLastUpdate();
//            nodeToCompareTime = new SimpleDateFormat(Engine.DATE_FORMAT).parse(nodeToCompareDate).getTime();
//            thisNodeTime = new SimpleDateFormat(Engine.DATE_FORMAT).parse(thisNodeDate).getTime();
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//
//        return (int)(nodeToCompareTime - thisNodeTime);
//    }
//
//    public Point getLocation(Graph i_Graph) {
//        return new Point((int) i_Graph.getGraphic(this).layoutXProperty().get(),
//                (int) i_Graph.getGraphic(this).layoutYProperty().get());
//    }
//
//    public Region getCommitDetails() {
//        return commitDetails;
//    }
//
//    public void setRectangleTreeNodeId(String i_CssId) {
//        rectangleTreeNode.setId(i_CssId);
//    }
//
//    public void setIdInList(int i_NodeId) {
//        idInList = i_NodeId;
//    }
//
//    public int getIdInList() { return idInList; }
//
//    @Override
//    public boolean equals(Object o) {
//        if(!(o instanceof CommitGraphNode)) {
//            return false;
//        }
//
//        return ((CommitGraphNode) o).getCommitNode().equals(commitNode);
//    }
//
//    public void setClickAction(Runnable i_Action) { commitDetails.setClickAction(i_Action); }
//}
//
