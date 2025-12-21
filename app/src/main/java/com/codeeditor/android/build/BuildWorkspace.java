package com.codeeditor.android.build;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BuildWorkspace {
    
    private Context context;
    private File workspaceRoot;
    private File projectsDir;
    private File sdkDir;
    private File templatesDir;
    
    public BuildWorkspace(Context context) {
        this.context = context;
        this.workspaceRoot = new File(context.getFilesDir(), "workspace");
        this.projectsDir = new File(workspaceRoot, "projects");
        this.sdkDir = new File(workspaceRoot, "sdk");
        this.templatesDir = new File(workspaceRoot, "templates");
        
        initializeDirectories();
    }
    
    private void initializeDirectories() {
        workspaceRoot.mkdirs();
        projectsDir.mkdirs();
        sdkDir.mkdirs();
        templatesDir.mkdirs();
    }
    
    public File createProject(String projectName, String packageName) throws IOException {
        String safeName = projectName.replaceAll("[^a-zA-Z0-9_-]", "_");
        File projectDir = new File(projectsDir, safeName);
        
        if (projectDir.exists()) {
            throw new IOException("Project already exists: " + projectName);
        }
        
        projectDir.mkdirs();
        new File(projectDir, "src/main/java/" + packageName.replace('.', '/')).mkdirs();
        new File(projectDir, "src/main/res/layout").mkdirs();
        new File(projectDir, "src/main/res/values").mkdirs();
        new File(projectDir, "src/main/res/drawable").mkdirs();
        new File(projectDir, "src/main/res/mipmap-hdpi").mkdirs();
        new File(projectDir, "src/main/res/mipmap-mdpi").mkdirs();
        new File(projectDir, "src/main/res/mipmap-xhdpi").mkdirs();
        new File(projectDir, "src/main/res/mipmap-xxhdpi").mkdirs();
        new File(projectDir, "build").mkdirs();
        
        createDefaultManifest(projectDir, packageName);
        createDefaultMainActivity(projectDir, packageName, projectName);
        createDefaultLayout(projectDir);
        createDefaultStrings(projectDir, projectName);
        createDefaultColors(projectDir);
        createDefaultStyles(projectDir);
        createProjectConfig(projectDir, projectName, packageName);
        
        return projectDir;
    }
    
    private void createDefaultManifest(File projectDir, String packageName) throws IOException {
        String manifest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    package=\"" + packageName + "\">\n\n" +
            "    <application\n" +
            "        android:allowBackup=\"true\"\n" +
            "        android:label=\"@string/app_name\"\n" +
            "        android:supportsRtl=\"true\"\n" +
            "        android:theme=\"@style/AppTheme\">\n\n" +
            "        <activity\n" +
            "            android:name=\".MainActivity\"\n" +
            "            android:exported=\"true\">\n" +
            "            <intent-filter>\n" +
            "                <action android:name=\"android.intent.action.MAIN\" />\n" +
            "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
            "            </intent-filter>\n" +
            "        </activity>\n\n" +
            "    </application>\n\n" +
            "</manifest>";
        
        writeFile(new File(projectDir, "src/main/AndroidManifest.xml"), manifest);
    }
    
    private void createDefaultMainActivity(File projectDir, String packageName, String projectName) throws IOException {
        String activity = "package " + packageName + ";\n\n" +
            "import android.app.Activity;\n" +
            "import android.os.Bundle;\n" +
            "import android.widget.TextView;\n\n" +
            "public class MainActivity extends Activity {\n\n" +
            "    @Override\n" +
            "    protected void onCreate(Bundle savedInstanceState) {\n" +
            "        super.onCreate(savedInstanceState);\n" +
            "        setContentView(R.layout.activity_main);\n\n" +
            "        TextView textView = findViewById(R.id.textView);\n" +
            "        textView.setText(\"Hello from " + projectName + "!\");\n" +
            "    }\n" +
            "}\n";
        
        File javaDir = new File(projectDir, "src/main/java/" + packageName.replace('.', '/'));
        writeFile(new File(javaDir, "MainActivity.java"), activity);
    }
    
    private void createDefaultLayout(File projectDir) throws IOException {
        String layout = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\"\n" +
            "    android:orientation=\"vertical\"\n" +
            "    android:gravity=\"center\"\n" +
            "    android:padding=\"16dp\">\n\n" +
            "    <TextView\n" +
            "        android:id=\"@+id/textView\"\n" +
            "        android:layout_width=\"wrap_content\"\n" +
            "        android:layout_height=\"wrap_content\"\n" +
            "        android:text=\"Hello World!\"\n" +
            "        android:textSize=\"24sp\" />\n\n" +
            "</LinearLayout>";
        
        writeFile(new File(projectDir, "src/main/res/layout/activity_main.xml"), layout);
    }
    
    private void createDefaultStrings(File projectDir, String projectName) throws IOException {
        String strings = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<resources>\n" +
            "    <string name=\"app_name\">" + projectName + "</string>\n" +
            "</resources>";
        
        writeFile(new File(projectDir, "src/main/res/values/strings.xml"), strings);
    }
    
    private void createDefaultColors(File projectDir) throws IOException {
        String colors = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<resources>\n" +
            "    <color name=\"colorPrimary\">#6200EE</color>\n" +
            "    <color name=\"colorPrimaryDark\">#3700B3</color>\n" +
            "    <color name=\"colorAccent\">#03DAC5</color>\n" +
            "</resources>";
        
        writeFile(new File(projectDir, "src/main/res/values/colors.xml"), colors);
    }
    
    private void createDefaultStyles(File projectDir) throws IOException {
        String styles = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<resources>\n" +
            "    <style name=\"AppTheme\" parent=\"android:Theme.Material.Light.DarkActionBar\">\n" +
            "        <item name=\"android:colorPrimary\">@color/colorPrimary</item>\n" +
            "        <item name=\"android:colorPrimaryDark\">@color/colorPrimaryDark</item>\n" +
            "        <item name=\"android:colorAccent\">@color/colorAccent</item>\n" +
            "    </style>\n" +
            "</resources>";
        
        writeFile(new File(projectDir, "src/main/res/values/styles.xml"), styles);
    }
    
    private void createProjectConfig(File projectDir, String projectName, String packageName) throws IOException {
        String config = "{\n" +
            "  \"name\": \"" + projectName + "\",\n" +
            "  \"package\": \"" + packageName + "\",\n" +
            "  \"minSdk\": 26,\n" +
            "  \"targetSdk\": 34,\n" +
            "  \"versionCode\": 1,\n" +
            "  \"versionName\": \"1.0\",\n" +
            "  \"mainActivity\": \".MainActivity\"\n" +
            "}";
        
        writeFile(new File(projectDir, "project.json"), config);
    }
    
    private void writeFile(File file, String content) throws IOException {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
    
    public List<File> listProjects() {
        List<File> projects = new ArrayList<>();
        File[] files = projectsDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && new File(file, "project.json").exists()) {
                    projects.add(file);
                }
            }
        }
        return projects;
    }
    
    public boolean deleteProject(String projectName) {
        File projectDir = new File(projectsDir, projectName);
        if (projectDir.exists()) {
            return deleteRecursive(projectDir);
        }
        return false;
    }
    
    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }
    
    public File getWorkspaceRoot() { return workspaceRoot; }
    public File getProjectsDir() { return projectsDir; }
    public File getSdkDir() { return sdkDir; }
    public File getTemplatesDir() { return templatesDir; }
    
    public boolean isSdkInstalled() {
        File platformsDir = new File(sdkDir, "platforms");
        return platformsDir.exists() && platformsDir.listFiles() != null && platformsDir.listFiles().length > 0;
    }
    
    public long getWorkspaceSize() {
        return getDirectorySize(workspaceRoot);
    }
    
    private long getDirectorySize(File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    size += getDirectorySize(file);
                }
            }
        } else {
            size = dir.length();
        }
        return size;
    }
}
