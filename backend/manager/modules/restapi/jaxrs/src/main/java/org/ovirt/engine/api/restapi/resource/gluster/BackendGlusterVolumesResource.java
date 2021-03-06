package org.ovirt.engine.api.restapi.resource.gluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.common.util.QueryHelper;
import org.ovirt.engine.api.model.Cluster;
import org.ovirt.engine.api.model.Fault;
import org.ovirt.engine.api.model.GlusterBrick;
import org.ovirt.engine.api.model.GlusterVolume;
import org.ovirt.engine.api.model.GlusterVolumes;
import org.ovirt.engine.api.model.Option;
import org.ovirt.engine.api.resource.ClusterResource;
import org.ovirt.engine.api.resource.gluster.GlusterVolumeResource;
import org.ovirt.engine.api.resource.gluster.GlusterVolumesResource;
import org.ovirt.engine.api.restapi.logging.Messages;
import org.ovirt.engine.api.restapi.resource.AbstractBackendCollectionResource;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.gluster.CreateGlusterVolumeParameters;
import org.ovirt.engine.core.common.action.gluster.GlusterVolumeParameters;
import org.ovirt.engine.core.common.businessentities.gluster.AccessProtocol;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterBrickEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeEntity;
import org.ovirt.engine.core.common.businessentities.gluster.GlusterVolumeType;
import org.ovirt.engine.core.common.businessentities.gluster.TransportType;
import org.ovirt.engine.core.common.constants.gluster.GlusterConstants;
import org.ovirt.engine.core.common.interfaces.SearchType;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.common.queries.gluster.IdQueryParameters;

/**
 * Implementation of the "glustervolumes" resource
 */
public class BackendGlusterVolumesResource
        extends AbstractBackendCollectionResource<GlusterVolume, GlusterVolumeEntity>
        implements GlusterVolumesResource {

    public static final String[] SUB_COLLECTIONS = { "bricks" };
    private ClusterResource parent;

    public BackendGlusterVolumesResource() {
        super(GlusterVolume.class, GlusterVolumeEntity.class, SUB_COLLECTIONS);
    }

    public BackendGlusterVolumesResource(ClusterResource parent) {
        this();
        setParent(parent);
    }

    public ClusterResource getParent() {
        return parent;
    }

    public void setParent(ClusterResource parent) {
        this.parent = parent;
    }

    @Override
    public GlusterVolumes list() {
        String constraint = QueryHelper.getConstraint(getUriInfo(), "cluster = "
                + parent.get().getName(), GlusterVolume.class);
        return mapCollection(getBackendCollection(SearchType.GlusterVolume, constraint));
    }

    private GlusterVolumes mapCollection(List<GlusterVolumeEntity> entities) {
        GlusterVolumes collection = new GlusterVolumes();
        for (GlusterVolumeEntity entity : entities) {
            collection.getGlusterVolumes().add(addLinks(populate(map(entity), entity)));
        }
        return collection;
    }

    @Override
    protected GlusterVolume addParents(GlusterVolume volume) {
        volume.setCluster(new Cluster());
        volume.getCluster().setId(parent.get().getId());
        return volume;
    }

    @Override
    public Response add(GlusterVolume volume) {
        validateParameters(volume, "name", "volumeType", "bricks");

        validateEnumParameters(volume);

        validateAccessControl(volume);

        GlusterVolumeEntity volumeEntity = getMapper(GlusterVolume.class, GlusterVolumeEntity.class).map(volume, null);
        volumeEntity.setClusterId(asGuid(parent.get().getId()));
        mapBricks(volume, volumeEntity);

        return performCreation(VdcActionType.CreateGlusterVolume,
                new CreateGlusterVolumeParameters(volumeEntity),
                new QueryIdResolver(VdcQueryType.GetGlusterVolumeById, IdQueryParameters.class),
                true);
    }

    private void validateEnumParameters(GlusterVolume volume) {
        validateEnum(GlusterVolumeType.class, volume.getVolumeType());

        if (volume.isSetTransportTypes())
        {
            validateEnums(TransportType.class, volume.getTransportTypes().getTransportTypes());
        }

        if (volume.isSetAccessProtocols())
        {
            validateEnums(AccessProtocol.class, volume.getAccessProtocols().getAccessProtocols());
        }
    }

    private void validateAccessControl(GlusterVolume volume) {
        if (volume.isSetAccessControlList() && volume.getAccessControlList().isSetAccessControlList()
                && volume.isSetOptions() && volume.getOptions().isSetOptions())
        {
            for (Option option : volume.getOptions().getOptions())
            {
                if (option.getName().equals(GlusterConstants.OPTION_AUTH_ALLOW))
                {
                    List<String> acList = volume.getAccessControlList().getAccessControlList();
                    List<String> acOptionList = Arrays.asList(option.getValue().split(","));

                    if (acList.size() == acOptionList.size() && acList.containsAll(acOptionList))
                    {
                        return;
                    }
                    else
                    {
                        Fault fault = new Fault();
                        fault.setReason(localize(Messages.DUPLICATE_ACCESS_CONTROL_GLUSTER_VOLUME_REASON));
                        fault.setDetail(localize(Messages.DUPLICATE_ACCESS_CONTROL_GLUSTER_VOLUME_DETAIL));
                        Response response = Response.status(Response.Status.BAD_REQUEST).entity(fault).build();
                        throw new WebApplicationException(response);
                    }
                }
            }
        }
    }

    private void mapBricks(GlusterVolume volume, GlusterVolumeEntity volumeEntity) {
        List<GlusterBrickEntity> bricks = new ArrayList<GlusterBrickEntity>();
        for(GlusterBrick brick : volume.getBricks().getGlusterBricks()) {
            bricks.add(getMapper(GlusterBrick.class, GlusterBrickEntity.class).map(brick, null));
        }
        volumeEntity.setBricks(bricks);
    }

    @Override
    protected Response performRemove(String id) {
        return performAction(VdcActionType.DeleteGlusterVolume, new GlusterVolumeParameters(asGuid(id)));
    }

    @Override
    public GlusterVolumeResource getGlusterVolumeSubResource(String id) {
        return inject(new BackendGlusterVolumeResource(id, this));
    }
}
