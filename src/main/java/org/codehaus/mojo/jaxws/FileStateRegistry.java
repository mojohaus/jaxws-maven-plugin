package org.codehaus.mojo.jaxws;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.plugin.MojoFailureException;

import java.io.*;
import java.util.HashMap;

public class FileStateRegistry implements Serializable {

    private static final long serialVersionUID = 1L;

    private HashMap<String, String> fileStateMap = new HashMap<String, String>();

    private transient HashMap<String, String> newFileStateMap = new HashMap<String, String>();
    private transient File registryData;

    public FileStateRegistry(File registryData){
        this.registryData = registryData;
        registryData.getParentFile().mkdirs();
        // check if accessible
    }

    /**
     * Checks file state based on md5 sums
     * @param file to check state
     * @return <code>true</code> if file content differs from previous file state based on md5 sums otherwise
     * <code>false</code>
     * @throws MojoFailureException
     */
    public boolean isChanged(File file) throws MojoFailureException {
        try {
            String name = file.getCanonicalPath();
            if(newFileStateMap.containsKey(name)){
                return true;
            }

            String savedFileMd5 = fileStateMap.get(name);
            String newFileMd5 = md5Sum(file);
            if(savedFileMd5 != null && savedFileMd5.equals(newFileMd5)){
                return false;
            } else {
                newFileStateMap.put(name, newFileMd5);
            }
        } catch (IOException e) {
            throw new MojoFailureException("Unable to determine file state for " + file.getName(), e);
        }
        return true;
    }

    /**
     * Create MD5 checksum of file contents
     * @param file for which checksum will be calculated
     * @return MD5 sum string of file contents
     * @throws MojoFailureException
     */
    private String md5Sum(File file) throws MojoFailureException {
        try {
            return DigestUtils.md5Hex(new FileInputStream(file));
        } catch (IOException e) {
            throw new MojoFailureException("Unable to calculate md5 sum for file " + file.getName() );
        }
    }

    /**
     * Store checksum for provided file in persistent file
     * @param file for which state should be persisted
     * @throws MojoFailureException
     */
    public void store(File file) throws MojoFailureException {
        FileOutputStream fis = null;
        try {
            String name = file.getCanonicalPath();
            if(newFileStateMap.containsKey(name)) {
                fileStateMap.put(name, newFileStateMap.remove(name));

                fis = new FileOutputStream(registryData);
                ObjectOutputStream ois = new ObjectOutputStream(fis);
                ois.writeObject(this);
            }
        } catch (Exception e) {
            throw new MojoFailureException("Unable to save file state registry " + registryData.getName() );
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }
    }

    /**
     * Load persistent state from state file
     * @param registryData persistent state file
     * @return registry initialized from state file
     */
    public static FileStateRegistry load(File registryData) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(registryData);
            ObjectInputStream ois = new ObjectInputStream(fis);
            FileStateRegistry fr = (FileStateRegistry) ois.readObject();
            fr.registryData = registryData;
            fr.newFileStateMap = new HashMap<String, String>();
            return fr;
        } catch (Exception e) {
            return new FileStateRegistry(registryData);
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }
    }
}
