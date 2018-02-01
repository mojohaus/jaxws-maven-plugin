package org.codehaus.mojo.jaxws;

import javassist.ClassPool;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;
import org.apache.maven.plugin.MojoExecutionException;

import javax.jws.WebService;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class Sei {
    private static final ClassPool CLASS_POOL = ClassPool.getDefault();

    public final boolean isChanged;
    public final String name;
    public final File classFile;
    public final WebService webservice;

    public Sei(String name) {
        this.isChanged = true;
        this.name = name;
        this.classFile = null;
        this.webservice = null;
    }

    public Sei(boolean isChanged, String name, WebService webservice, File classFile) throws IOException {
        this.isChanged = isChanged;
        this.name = name;
        this.classFile = classFile.getCanonicalFile();
        this.webservice = webservice;
    }

    public File jaxwsDir(File classesDir){
        String[] nameParts = name.split("\\.");
        if(nameParts.length == 1){
            return  new File(classesDir, "jaxws");
        }
        StringBuffer sb = new StringBuffer(nameParts.length);
        for(int i = 0, len = nameParts.length - 1, last = len -1; i < len; i++){
            if(i == last) {
                sb.append(nameParts[i]).append(File.separator).append("jaxws");
            } else {
                sb.append(nameParts[i]).append(File.separator);
            }
        }
        return new File(classesDir, sb.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sei sei = (Sei) o;

        if (!name.equals(sei.name)) return false;
        return classFile != null ? classFile.equals(sei.classFile) : sei.classFile == null;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (classFile != null ? classFile.hashCode() : 0);
        return result;
    }

    public static class Type {
        public final ClassFile classFile;
        public final WebService webservice;

        public Type(ClassFile classFile, WebService webservice){
            this.classFile = classFile;
            this.webservice = webservice;
        }
    }

    public static Type findWebserviceAnnotation(File file) throws IOException, MojoExecutionException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            ClassFile classFile = new ClassFile(new DataInputStream(fis));
            Type result = null;
            AnnotationsAttribute visible = (AnnotationsAttribute) classFile.getAttribute(AnnotationsAttribute.visibleTag);
            AnnotationsAttribute invisible = (AnnotationsAttribute) classFile.getAttribute(AnnotationsAttribute.invisibleTag);
            result = findWebServiceAnnotationForAnnotationsAttribute(classFile, visible);
            if(result == null) {
                result = findWebServiceAnnotationForAnnotationsAttribute(classFile, invisible);
            }
            return result;
        } finally {
            if(fis != null){
                try {
                    fis.close();
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }
    }

    private static Type findWebServiceAnnotationForAnnotationsAttribute(ClassFile classFile, AnnotationsAttribute attr) throws MojoExecutionException {
        if (attr != null) {
            Annotation[] anns = attr.getAnnotations();

            for (javassist.bytecode.annotation.Annotation next : anns) {
                if(!next.getTypeName().equals(WebService.class.getName()))
                    continue;
                try {
                    return new Type(classFile, (WebService) (next.toAnnotationType(Sei.class.getClassLoader(), CLASS_POOL)));
                } catch (ClassNotFoundException e) {
                    throw new MojoExecutionException("Problem finding class for annotation: " + e.getMessage(), e);
                }
            }
        }
        return null;
    }
}
