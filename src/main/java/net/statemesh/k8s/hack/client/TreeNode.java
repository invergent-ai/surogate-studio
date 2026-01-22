package net.statemesh.k8s.hack.client;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/** Helper class to represent directory tree */
public class TreeNode {
    boolean isDir;
    String name;
    boolean isRoot;
    List<TreeNode> children;

    public TreeNode(boolean isDir, String name, boolean isRoot) {
        this.isDir = isDir;
        this.name = name;
        this.isRoot = isRoot;
        this.children = new ArrayList<>();
    }

    public boolean isDir() {
        return isDir;
    }

    public void setDir(boolean dir) {
        isDir = dir;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this).toString();
    }
}
