package parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SyntaxNode {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger(1);

    private final int id;
    private final String label;
    private final List<SyntaxNode> children = new ArrayList<>();

    public SyntaxNode(String label) {
        this.id = ID_COUNTER.getAndIncrement();
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void addChild(SyntaxNode child) {
        children.add(child);
    }

    public List<SyntaxNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

 
    public void print(int indent) {
        printTree();
    }

    
    public void printTree() {
        printTree("", true);
    }

    // internal recursive printer
    private void printTree(String prefix, boolean isTail) {
        // Root node prints without leading connector when prefix is empty and isTail==true
        String connector = prefix.isEmpty() ? "" : (isTail ? "└─ " : "├─ ");
        System.out.printf("%s%s (id=%04d)%n", prefix + connector, label, id);

        List<SyntaxNode> kids = this.getChildren();
        for (int i = 0; i < kids.size(); i++) {
            boolean last = (i == kids.size() - 1);
            SyntaxNode child = kids.get(i);
            String childPrefix;
            if (prefix.isEmpty()) {
                // top-level: no leading vertical bar yet
                childPrefix = last ? "    " : "│   ";
            } else {
                childPrefix = prefix + (isTail ? "    " : "│   ");
            }
            child.printTree(childPrefix, last);
        }
    }
}
