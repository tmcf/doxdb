package net.trajano.doxb.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Date;

import javax.persistence.PersistenceException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;

import net.trajano.doxdb.DoxConfiguration;
import net.trajano.doxdb.DoxID;
import net.trajano.doxdb.DoxImportBuilder;
import net.trajano.doxdb.jdbc.DoxPrincipal;
import net.trajano.doxdb.jdbc.JdbcDoxDAO;

public class JdbcTest {

    // System.out.println(ID.generate());
    // System.out.println(ID.generate());
    // System.out.println(ID.generate());
    // System.out.println(ID.generate());
    // System.out.println(ID.generate());
    //
    // for (char i = 0x21; i < 0x7f; ++i) {
    // System.out.print(i);
    // }
    // // EntityManagerFactory emf =
    // // PersistenceProviderResolverHolder.getPersistenceProviderResolver()
    // // .getPersistenceProviders()
    // // .get(0)
    // // .createContainerEntityManagerFactory(new DoxdbPersistenceUnitInfo(),
    // // ImmutableMap.builder()
    // // .put("javax.persistence.jdbc.driver",
    // // "org.apache.derby.jdbc.EmbeddedDriver")
    // // .put("javax.persistence.schema-generation.database.action", "create")
    // // .put("javax.persistence.jdbc.url", "jdbc:derby:memory:" +
    // // UUID.randomUUID() + ";create=true")
    // // .put("eclipselink.logging.logger", "JavaLogger")
    // // .put("eclipselink.logging.level.sql", "fine")
    // // .put("eclipselink.logging.parameters", "true")
    // // .build());
    // //
    // // emf.createEntityManager();
    // // System.out.println(emf);
    // // emf.close();
    // }

    @Test
    public void testDoubleCreateDerby() throws Exception {

        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        final Connection c = DriverManager.getConnection("jdbc:derby:memory:" + DoxID.generate() + ";create=true");

        // Connection c = DriverManager.getConnection("jdbc:derby://" +
        // InetAddress.getLocalHost()
        // .getHostName() +
        // ":1527/sun-appserv-samples;create=true;upgrade=true");

        final JdbcDoxDAO dao = new JdbcDoxDAO(c, "sample");
        final JdbcDoxDAO dao2 = new JdbcDoxDAO(c, "sample");
        final DoxID d1 = dao.create(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                .getInput(), new DoxPrincipal("PRINCE"));
        final DoxID d2 = dao2.create(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                .getInput(), new DoxPrincipal("PRINCE"));
        Assert.assertFalse(d1.equals(d2));
    }

    @Test
    public void testDoubleCreateDerbyWithOOB() throws Exception {

        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        final Connection c = DriverManager.getConnection("jdbc:derby:memory:" + DoxID.generate() + ";create=true");

        DoxConfiguration config = new DoxConfiguration("sample");
        config.setHasOob(true);

        // Connection c = DriverManager.getConnection("jdbc:derby://" +
        // InetAddress.getLocalHost()
        // .getHostName() +
        // ":1527/sun-appserv-samples;create=true;upgrade=true");

        final JdbcDoxDAO dao = new JdbcDoxDAO(c, config);
        final JdbcDoxDAO dao2 = new JdbcDoxDAO(c, config);
        final DoxID d1 = dao.create(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                .getInput(), new DoxPrincipal("PRINCE"));
        final DoxID d2 = dao2.create(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                .getInput(), new DoxPrincipal("PRINCE"));
        Assert.assertFalse(d1.equals(d2));
    }

    @Test
    public void testDoubleCreateH2() throws Exception {

        Class.forName("org.h2.Driver");

        // Connection c = DriverManager.getConnection("jdbc:derby://" +
        // InetAddress.getLocalHost()
        // .getHostName() +
        // ":1527/sun-appserv-samples;create=true;upgrade=true");
        final DoxID d1;
        {
            final Connection c = DriverManager.getConnection("jdbc:h2:mem:test");
            final JdbcDoxDAO dao = new JdbcDoxDAO(c, "sample");
            d1 = dao.create(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                    .getInput(), new DoxPrincipal("PRINCE"));
            c.commit();
            c.close();
        }

        {
            final Connection c = DriverManager.getConnection("jdbc:h2:mem:test");
            final JdbcDoxDAO dao2 = new JdbcDoxDAO(c, "sample");
            final DoxID d2 = dao2.create(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                    .getInput(), new DoxPrincipal("PRINCE"));
            Assert.assertFalse(d1.equals(d2));
        }
    }

    @Test(expected = PersistenceException.class)
    public void testFailDoubleImport() throws Exception {

        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        final Connection c = DriverManager.getConnection("jdbc:derby:memory:" + DoxID.generate() + ";create=true");

        // Connection c = DriverManager.getConnection("jdbc:derby://" +
        // InetAddress.getLocalHost()
        // .getHostName() +
        // ":1527/sun-appserv-samples;create=true;upgrade=true");

        try {
            final JdbcDoxDAO dao = new JdbcDoxDAO(c, "sample");
            DoxID d1 = new DoxID("012345678901234567890123456789AB");

            DoxImportBuilder b = new DoxImportBuilder().contentStream(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                    .getInput())
                    .doxID(d1)
                    .createdBy(new DoxPrincipal("FRESH"))
                    .createdOn(new Date(5))
                    .lastUpdatedBy(new DoxPrincipal("PRINCE"))
                    .lastUpdatedOn(new Date(50));

            dao.importDox(b);
            dao.importDox(b);
            c.commit();
        } finally {
            c.close();
        }
    }

    @Test
    public void testImport() throws Exception {

        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        final Connection c = DriverManager.getConnection("jdbc:derby:memory:" + DoxID.generate() + ";create=true");

        // Connection c = DriverManager.getConnection("jdbc:derby://" +
        // InetAddress.getLocalHost()
        // .getHostName() +
        // ":1527/sun-appserv-samples;create=true;upgrade=true");

        final JdbcDoxDAO dao = new JdbcDoxDAO(c, "sample");
        DoxID d1 = new DoxID("012345678901234567890123456789AB");
        DoxImportBuilder b = new DoxImportBuilder().contentStream(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                .getInput())
                .doxID(d1)
                .createdBy(new DoxPrincipal("FRESH"))
                .createdOn(new Date(5))
                .lastUpdatedBy(new DoxPrincipal("PRINCE"))
                .lastUpdatedOn(new Date(50));

        dao.importDox(b);

        dao.updateContent(d1, Resources.newInputStreamSupplier(Resources.getResource("sample.bin"))
                .getInput(), dao.getVersion(d1), new DoxPrincipal("PRINCEUP"));

        final byte[] buffer1 = new byte[5000];
        ByteStreams.readFully(dao.readContent(d1), buffer1);
        final byte[] buffer2 = new byte[5000];
        ByteStreams.readFully(Resources.newInputStreamSupplier(Resources.getResource("sample.bin"))
                .getInput(), buffer2);
        Assert.assertArrayEquals(buffer1, buffer2);
        final int d1Version = dao.getVersion(d1);
        dao.delete(d1, d1Version, new DoxPrincipal("PRINCE"));
        c.commit();
        c.close();
    }

    @Test
    public void testImportTwo() throws Exception {

        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        final Connection c = DriverManager.getConnection("jdbc:derby:memory:" + DoxID.generate() + ";create=true");

        // Connection c = DriverManager.getConnection("jdbc:derby://" +
        // InetAddress.getLocalHost()
        // .getHostName() +
        // ":1527/sun-appserv-samples;create=true;upgrade=true");

        final JdbcDoxDAO dao = new JdbcDoxDAO(c, "sample");
        DoxID d1 = new DoxID("012345678901234567890123456789AB");
        DoxImportBuilder b = new DoxImportBuilder().contentStream(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                .getInput())
                .doxID(d1)
                .createdBy(new DoxPrincipal("FRESH"))
                .createdOn(new Date(5))
                .lastUpdatedBy(new DoxPrincipal("PRINCE"))
                .lastUpdatedOn(new Date(50));

        dao.importDox(b);

        b.doxID(new DoxID("012345678901234567890123456789AC"));
        dao.importDox(b);

        dao.updateContent(d1, Resources.newInputStreamSupplier(Resources.getResource("sample.bin"))
                .getInput(), dao.getVersion(d1), new DoxPrincipal("PRINCEUP"));

        final byte[] buffer1 = new byte[5000];
        ByteStreams.readFully(dao.readContent(d1), buffer1);
        final byte[] buffer2 = new byte[5000];
        ByteStreams.readFully(Resources.newInputStreamSupplier(Resources.getResource("sample.bin"))
                .getInput(), buffer2);
        Assert.assertArrayEquals(buffer1, buffer2);
        final int d1Version = dao.getVersion(d1);
        dao.delete(d1, d1Version, new DoxPrincipal("PRINCE"));
        c.commit();
        c.close();
    }

    @Test
    public void testPersistence() throws Exception {

        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        final Connection c = DriverManager.getConnection("jdbc:derby:memory:" + DoxID.generate() + ";create=true");

        // Connection c = DriverManager.getConnection("jdbc:derby://" +
        // InetAddress.getLocalHost()
        // .getHostName() +
        // ":1527/sun-appserv-samples;create=true;upgrade=true");

        final JdbcDoxDAO dao = new JdbcDoxDAO(c, "sample");
        final DoxID d1 = dao.create(Resources.newInputStreamSupplier(Resources.getResource("sample.bin"))
                .getInput(), new DoxPrincipal("PRINCE"));
        final DoxID d2 = dao.create(Resources.newInputStreamSupplier(Resources.getResource("sample.bin"))
                .getInput(), new DoxPrincipal("PRINCE"));
        Assert.assertFalse(d1.equals(d2));
        final byte[] buffer1 = new byte[5000];
        ByteStreams.readFully(dao.readContent(d1), buffer1);
        final byte[] buffer2 = new byte[5000];
        ByteStreams.readFully(Resources.newInputStreamSupplier(Resources.getResource("sample.bin"))
                .getInput(), buffer2);
        Assert.assertArrayEquals(buffer1, buffer2);
        final int d1Version = dao.getVersion(d1);
        dao.delete(d1, d1Version, new DoxPrincipal("PRINCE"));
        c.commit();
        c.close();
    }

    @Test
    public void testUpdate() throws Exception {

        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        final Connection c = DriverManager.getConnection("jdbc:derby:memory:" + DoxID.generate() + ";create=true");

        // Connection c = DriverManager.getConnection("jdbc:derby://" +
        // InetAddress.getLocalHost()
        // .getHostName() +
        // ":1527/sun-appserv-samples;create=true;upgrade=true");

        final JdbcDoxDAO dao = new JdbcDoxDAO(c, "sample");
        final DoxID d1 = dao.create(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                .getInput(), new DoxPrincipal("PRINCE"));
        final DoxID d2 = dao.create(Resources.newInputStreamSupplier(Resources.getResource("sample.xml"))
                .getInput(), new DoxPrincipal("PRINCE"));
        Assert.assertFalse(d1.equals(d2));

        dao.updateContent(d1, Resources.newInputStreamSupplier(Resources.getResource("sample.bin"))
                .getInput(), dao.getVersion(d1), new DoxPrincipal("PRINCEUP"));

        final byte[] buffer1 = new byte[5000];
        ByteStreams.readFully(dao.readContent(d1), buffer1);
        final byte[] buffer2 = new byte[5000];
        ByteStreams.readFully(Resources.newInputStreamSupplier(Resources.getResource("sample.bin"))
                .getInput(), buffer2);
        Assert.assertArrayEquals(buffer1, buffer2);
        final int d1Version = dao.getVersion(d1);
        dao.delete(d1, d1Version, new DoxPrincipal("PRINCE"));
        c.commit();
        c.close();
    }
}
