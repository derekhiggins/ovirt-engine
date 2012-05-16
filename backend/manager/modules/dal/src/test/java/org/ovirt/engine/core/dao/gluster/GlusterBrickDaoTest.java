package org.ovirt.engine.core.dao.gluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.VdsStatic;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickStatus;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dao.BaseDAOTestCase;

public class GlusterBrickDaoTest extends BaseDAOTestCase {
    private static final Guid SERVER_ID = new Guid("afce7a39-8e8c-4819-ba9c-796d316592e6");
    private static final Guid EXISTING_VOL_ID = new Guid("0c3f45f6-3fe9-4b35-a30c-be0d1a835ea8");
    private static final Guid EXISTING_BRICK_ID = new Guid("6ccdc294-d77b-4929-809d-8afe7634b47d");

    private GlusterBrickDao dao;
    private VdsStatic server;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dao = dbFacade.getGlusterBrickDao();
        server = dbFacade.getVdsStaticDAO().get(SERVER_ID);
    }

    @Test
    public void testSaveAndGetById() {
        GlusterBrickEntity brickToAdd = new GlusterBrickEntity(EXISTING_VOL_ID,
                server,
                "/export/test-vol-distribute-1/dir3",
                GlusterBrickStatus.UP);

        dao.save(brickToAdd);

        GlusterBrickEntity retrievedBrick = dao.getById(brickToAdd.getId());
        assertNotNull(retrievedBrick);
        assertEquals(brickToAdd, retrievedBrick);
    }

    @Test
    public void testRemove() {
        GlusterBrickEntity existingBrick = dao.getById(EXISTING_BRICK_ID);
        assertNotNull(existingBrick);

        dao.removeBrick(EXISTING_BRICK_ID);

        assertNull(dao.getById(EXISTING_BRICK_ID));
    }

    @Test
    public void testReplaceBrick() {
        GlusterBrickEntity firstBrick = dao.getById(EXISTING_BRICK_ID);
        assertNotNull(firstBrick);

        GlusterBrickEntity newBrick =
                new GlusterBrickEntity(EXISTING_VOL_ID,
                        server,
                        "/export/test-vol-distribute-1/dir3",
                        GlusterBrickStatus.UP);

        assertNull(dao.getById(newBrick.getId()));

        dao.replaceBrick(firstBrick, newBrick);

        assertNull(dao.getById(EXISTING_BRICK_ID));

        GlusterBrickEntity retrievedBrick = dao.getById(newBrick.getId());
        assertNotNull(retrievedBrick);
        assertEquals(newBrick, retrievedBrick);
    }

    @Test
    public void testUpdateBrickStatus() {
        GlusterBrickEntity existingBrick = dao.getById(EXISTING_BRICK_ID);
        assertNotNull(existingBrick);
        assertEquals(GlusterBrickStatus.UP, existingBrick.getStatus());

        dao.updateBrickStatus(EXISTING_BRICK_ID, GlusterBrickStatus.DOWN);

        existingBrick = dao.getById(EXISTING_BRICK_ID);
        assertNotNull(existingBrick);
        assertEquals(GlusterBrickStatus.DOWN, existingBrick.getStatus());
    }
}