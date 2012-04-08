package rstar;

import rstar.dto.NodeDTO;
import rstar.dto.PointDTO;
import rstar.dto.TreeDTO;
import rstar.interfaces.IDiskQuery;
import rstar.interfaces.IRStarNode;
import util.Constants;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * User: Lokesh
 * Date: 3/4/12
 * Time: 1:33 AM
 */
public class StorageManager implements IDiskQuery {
    RandomAccessFile dataStore;
    RandomAccessFile nodeFile;
    FileChannel dataChannel;
    FileChannel nodeChannel;


    public StorageManager() {
        try {
            dataStore = new RandomAccessFile(Constants.DATA_FILE, "rw");
            dataChannel = dataStore.getChannel();
        } catch (FileNotFoundException e) {
            System.err.println("Data File failed to be loaded/created. Exiting");
            System.exit(1);
        }
    }

    @Override
    public void save(IRStarNode node) {
        if (node.isLeaf()) {
            try {
                RStarLeaf leaf = (RStarLeaf) node;

                if (leaf.hasUnsavedPoints()) {
                    //save unsaved points to disk first.
                    int firstUnsaved = leaf.indexOfFirstUnsavedPoint();
                    assert firstUnsaved != -1;
                    for (int i = firstUnsaved; i < leaf.children.size(); i++) {
                        leaf.childPointers[i] = savePoint(leaf.children.get(i).toDTO());
                    }
                }

                nodeFile = new RandomAccessFile(constructFilename(leaf.nodeId), "w");
                nodeChannel = nodeFile.getChannel();
                nodeFile.seek(0);       //overwrite the file

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(leaf.toDTO());
                oos.flush();

                nodeChannel.write(ByteBuffer.wrap(bos.toByteArray()));
                oos.close();

            } catch (FileNotFoundException e) {
                System.err.println("Exception while saving node to disk");
            } catch (IOException e) {
                System.err.println("Exception while saving node to disk");
            }
        } else {
            try {
                RStarInternal internal = (RStarInternal) node;

                updateChannel(new File(constructFilename(internal.nodeId)));
                nodeFile.seek(0);       //overwrite the file

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(internal.toDTO());
                oos.flush();

                nodeChannel.write(ByteBuffer.wrap(bos.toByteArray()));
                oos.close();

            } catch (FileNotFoundException e) {
                System.err.println("Exception while saving node to disk");
            } catch (IOException e) {
                System.err.println("Exception while saving node to disk");
            }
        }
    }

    @Override
    public RStarNode load(long nodeId) throws FileNotFoundException {
        return nodeFromDisk(constructFilename(nodeId));
    }

    /**
     * saves a Spatial Point to dataFile on disk and
     * returns the offset of the point in the file.
     *
     * @param pointDTO DTO of the point to be saved
     * @return the location where the point was saved in
     * datafile
     */
    @Override
    public long savePoint(PointDTO pointDTO) {
        try {
            long pos = dataStore.length();
            dataStore.seek(pos);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(pointDTO);
            oos.flush();

            dataChannel.write(ByteBuffer.wrap(bos.toByteArray()));
            oos.close();
            return pos;
        } catch (IOException e) {
            System.err.println("Exception occurred while saving data to disk.");
            return -1;
        }
    }

    /**
     * loads a SpatialPoint from dataFile
     * @param pointer the offset of the point
     *                in dataFile
     * @return DTO of the point. Full SpatialPoint
     * can be easily constructed from the DTO
     */
    @Override
    public PointDTO loadPoint(long pointer) {
        try {
            dataStore.seek(pointer);
            ObjectInputStream ois = getPointObjectStream();
            PointDTO pointDTO = (PointDTO) ois.readObject();
            ois.close();
            return pointDTO;

        } catch (IOException e) {
            System.err.println("Exception occurred while loading point from disk.");
        } catch (ClassNotFoundException e) {
            System.err.println("Exception occurred while loading point from disk.");
        }
        return null;
    }

    private RStarNode nodeFromDisk(String filename) throws FileNotFoundException {
        try {
            updateChannel(new File(filename));
            nodeFile.seek(0);
            ObjectInputStream ois = getNodeObjectStream();
            NodeDTO dto = (NodeDTO) ois.readObject();
            ois.close();

            RStarNode result;
            if(dto.isLeaf)
                result = new RStarLeaf(dto, nodeIdFromFilename(filename));
            else
                result = new RStarInternal(dto, nodeIdFromFilename(filename));

            return result;
        } catch (IOException e) {
            System.err.println("Exception occurred while loading node from disk");
        } catch (ClassNotFoundException e) {
            System.err.println("Exception occurred while loading node from disk");
        }

        return null;
    }

    /**
     * saves the R* Tree to saveFile.
     * doesn't use RandomAccessFile
     * @param tree the DTO of the tree to be saved
     * @param saveFile save file location
     * @return 1 is successful, else -1
     */
    @Override
    public int saveTree(TreeDTO tree, File saveFile) {
        int status = -1;
        try {
            if(saveFile.exists()) {
                saveFile.delete();
            }

            FileOutputStream fos = new FileOutputStream(saveFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(tree);
            oos.flush();
            oos.close();
            status = 1;             // successful save
        } catch (IOException e) {
            System.err.println("Error while saving Tree to " + saveFile.toURI());
        }
        return status;
    }

    /**
     * loads a R* Tree from disk
     * @param saveFile the file to load the tree from
     * @return DTO of the loaded R* Tree, null if none found
     * @throws FileNotFoundException
     */
    @Override
    public TreeDTO loadTree(File saveFile) throws FileNotFoundException {
        try {
            FileInputStream fis = new FileInputStream(saveFile);
            ObjectInputStream ois = new ObjectInputStream(fis);

            TreeDTO dto = (TreeDTO) ois.readObject();
            return dto;

        } catch (IOException e) {
            System.err.println("Exception while loading tree from " + saveFile);
        } catch (ClassNotFoundException e) {
            System.err.println("Exception while loading tree from " + saveFile);
        }
        return null;
    }

    private void updateChannel(File file) throws FileNotFoundException {
        nodeFile = new RandomAccessFile(file, "rw");
        nodeChannel = nodeFile.getChannel();
    }

    public String constructFilename(long nodeId) {
        String file = Constants.FILE_PREFIX + nodeId + Constants.FILE_SUFFIX;
        return file;
    }

    public long nodeIdFromFilename(String filename) {
        int i2 = filename.indexOf(Constants.FILE_SUFFIX);
        assert i2 != -1;
        return Long.parseLong(filename.substring(Constants.FILE_PREFIX.length(), i2));
    }

    private ObjectInputStream getPointObjectStream() throws IOException {
        return new ObjectInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                return dataStore.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return dataStore.read(b, off, len);
            }
        });
    }

    private ObjectInputStream getNodeObjectStream() throws IOException {
        return new ObjectInputStream(new InputStream() {
            @Override
            public int read() throws IOException {
                return nodeFile.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return nodeFile.read(b, off, len);
            }
        });
    }
}
