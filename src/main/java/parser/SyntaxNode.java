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

    /**
     * Backwards-compatible print method (delegates to pretty printer).
     * Keep the same signature so existing calls remain valid.
     */
    public void print(int indent) {
        printTree();
    }

    /**
     * Pretty-print the tree using ASCII connectors and zero-padded ids.
     * Example:
     * Program (id=0001)
     * ├─ glob (id=0002)
     * │  └─ x (id=0003)
     * └─ main (id=0004)
     */
    public void printTree() {
        printTree("", true);
    }

    // Internal recursive printer: prefix is accumulated, isTail marks last child.
    private void printTree(String prefix, boolean isTail) {
        String connector = prefix.isEmpty() ? "" : (isTail ? "└─ " : "├─ ");
        System.out.printf("%s%s (id=%04d)%n", prefix + connector, label, id);

        List<SyntaxNode> kids = this.getChildren();
        for (int i = 0; i < kids.size(); i++) {
            boolean last = (i == kids.size() - 1);
            SyntaxNode child = kids.get(i);
            String childPrefix;
            if (prefix.isEmpty()) {
                childPrefix = last ? "    " : "│   ";
            } else {
                childPrefix = prefix + (isTail ? "    " : "│   ");
            }
            child.printTree(childPrefix, last);
        }
    }
}
