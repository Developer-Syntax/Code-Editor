package com.codeeditor.android.build.tasks;

import com.codeeditor.android.build.BuildContext;
import com.codeeditor.android.build.BuildException;
import com.codeeditor.android.build.BuildTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class SignApkTask implements BuildTask {
    
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private int progress = 0;
    private String progressMessage = "Signing APK...";
    
    @Override
    public String getName() {
        return "Sign APK";
    }
    
    @Override
    public boolean execute(BuildContext context) throws BuildException {
        context.phaseStarted("APK Signing");
        
        try {
            File unsignedApk = context.getUnsignedApk();
            File outputApk = context.getConfig().getOutputApk();
            
            if (!unsignedApk.exists()) {
                throw new BuildException("Sign", "Unsigned APK not found: " + unsignedApk);
            }
            
            outputApk.getParentFile().mkdirs();
            
            context.log("Generating debug signing key...");
            KeyPair keyPair = generateDebugKey();
            
            context.log("Signing APK with v1 signature...");
            signApkV1(context, unsignedApk, outputApk, keyPair.getPrivate());
            
            context.putArtifact("signed.apk", outputApk);
            context.log("Signed APK created: " + outputApk.getName() + 
                " (" + outputApk.length() / 1024 + " KB)");
            context.phaseCompleted("APK Signing");
            
            return true;
            
        } catch (Exception e) {
            throw new BuildException("Sign", "APK signing failed: " + e.getMessage(), e);
        }
    }
    
    private KeyPair generateDebugKey() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }
    
    private void signApkV1(BuildContext context, File inputApk, File outputApk, PrivateKey privateKey) 
            throws Exception {
        
        progress = 10;
        progressMessage = "Computing file digests...";
        context.progress(progress, progressMessage);
        
        Manifest manifest = new Manifest();
        Attributes mainAttrs = manifest.getMainAttributes();
        mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttrs.put(new Attributes.Name("Created-By"), "Code Editor Build System");
        
        try (JarFile jarFile = new JarFile(inputApk)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                if (cancelled.get()) return;
                
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) {
                    continue;
                }
                
                String digest = computeDigest(jarFile.getInputStream(entry));
                Attributes attrs = new Attributes();
                attrs.putValue("SHA-256-Digest", digest);
                manifest.getEntries().put(entry.getName(), attrs);
            }
        }
        
        progress = 40;
        progressMessage = "Creating signature files...";
        context.progress(progress, progressMessage);
        
        ByteArrayOutputStream manifestBytes = new ByteArrayOutputStream();
        manifest.write(manifestBytes);
        byte[] manifestData = manifestBytes.toByteArray();
        
        StringBuilder signatureFile = new StringBuilder();
        signatureFile.append("Signature-Version: 1.0\r\n");
        signatureFile.append("SHA-256-Digest-Manifest: ");
        signatureFile.append(computeDigestBytes(manifestData));
        signatureFile.append("\r\n");
        signatureFile.append("Created-By: Code Editor Build System\r\n\r\n");
        
        byte[] signatureFileData = signatureFile.toString().getBytes("UTF-8");
        
        progress = 60;
        progressMessage = "Generating signature block...";
        context.progress(progress, progressMessage);
        
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(signatureFileData);
        byte[] signatureBlock = signature.sign();
        
        progress = 80;
        progressMessage = "Writing signed APK...";
        context.progress(progress, progressMessage);
        
        try (JarFile jarFile = new JarFile(inputApk);
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputApk))) {
            
            jos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            jos.write(manifestData);
            jos.closeEntry();
            
            jos.putNextEntry(new ZipEntry("META-INF/CERT.SF"));
            jos.write(signatureFileData);
            jos.closeEntry();
            
            jos.putNextEntry(new ZipEntry("META-INF/CERT.RSA"));
            writeSignatureBlock(jos, signatureBlock);
            jos.closeEntry();
            
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                if (cancelled.get()) return;
                
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("META-INF/")) {
                    continue;
                }
                
                jos.putNextEntry(new ZipEntry(entry.getName()));
                copyStream(jarFile.getInputStream(entry), jos);
                jos.closeEntry();
            }
        }
        
        progress = 100;
        progressMessage = "APK signed successfully";
        context.progress(progress, progressMessage);
    }
    
    private String computeDigest(InputStream input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        return Base64.getEncoder().encodeToString(md.digest());
    }
    
    private String computeDigestBytes(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data);
        return Base64.getEncoder().encodeToString(md.digest());
    }
    
    private void writeSignatureBlock(OutputStream out, byte[] signature) throws IOException {
        out.write(signature);
    }
    
    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }
    
    @Override
    public void cancel() {
        cancelled.set(true);
    }
    
    @Override
    public int getProgress() {
        return progress;
    }
    
    @Override
    public String getProgressMessage() {
        return progressMessage;
    }
}
