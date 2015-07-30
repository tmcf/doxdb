package net.trajano.doxdb.search.lucene;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.persistence.EntityManager;

import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

public class JpaDirectory extends BaseDirectory {

    /**
     * Directory name.
     */
    private final String directoryName;

    private final EntityManager em;

    public JpaDirectory(final EntityManager em,
        final String directoryName) {
        super(new JpaLockFactory(em, directoryName));
        this.em = em;
        this.directoryName = directoryName;

    }

    @Override
    public void close() throws IOException {

        isOpen = false;

    }

    @Override
    public IndexOutput createOutput(final String name,
        final IOContext context) throws IOException {

        return new JpaIndexOutput(name, em, new ByteArrayOutputStream(), directoryName);

    }

    @Override
    public void deleteFile(final String name) throws IOException {

        em.remove(em.find(DoxSearchIndex.class, new DirectoryFile(directoryName, name)));
    }

    @Override
    public long fileLength(final String name) throws IOException {

        return em.find(DoxSearchIndex.class, new DirectoryFile(directoryName, name)).getContentLength();
    }

    @Override
    public String[] listAll() throws IOException {

        return em.createNamedQuery("searchListAll", String.class).setParameter("directoryName", directoryName).getResultList().toArray(new String[0]);
    }

    @Override
    public IndexInput openInput(final String name,
        final IOContext context) throws IOException {

        final DoxSearchIndex entry = em.find(DoxSearchIndex.class, new DirectoryFile(directoryName, name));
        if (entry == null) {
            return null;
        }

        return new ByteArrayIndexInput(name, context, entry.getContent());
    }

    @Override
    public void renameFile(final String source,
        final String dest) throws IOException {

        if (source.equals(dest)) {
            return;
        }

        final DoxSearchIndex src = em.find(DoxSearchIndex.class, new DirectoryFile(directoryName, source));

        final DoxSearchIndex entry = new DoxSearchIndex();
        entry.setContent(src.getContent());
        entry.setContentLength(src.getContentLength());
        final DirectoryFile directoryFile = new DirectoryFile();
        directoryFile.setDirectoryName(directoryName);
        directoryFile.setFileName(dest);
        entry.setDirectoryFile(directoryFile);
        em.remove(src);
        em.persist(entry);

    }

    /**
     * Flushes the entity manager.
     */
    @Override
    public void sync(final Collection<String> names) throws IOException {

        em.flush();
    }

}
