package net.veryuniqueusrnm.bedloader.client;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;

import java.util.*;

public class GeometryUtil {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    // Copied from CubeListBuilder
    private static final Set<Direction> ALL_VISIBLE = EnumSet.allOf(Direction.class);

    public static BedrockPlayerEntityModel<AbstractClientPlayer> bedrockGeoToJava(SkinInfo info) {
        // There are some times when the skin image file is larger than the geometry UV points.
        // In this case, we need to scale UV calls
        // https://github.com/Camotoy/BedrockSkinUtility/issues/9
        int uvHeight = info.getHeight();
        int uvWidth = info.getWidth();

        // Construct a list of all bones we need to translate
        List<JsonObject> bones = new ArrayList<>();
        try {
            String geometryName = info.getGeometryName().getAsJsonObject("geometry").get("default").getAsString();
            if (info.getGeometry().get("format_version").getAsString().equals("1.8.0")) {
                for (JsonElement node : info.getGeometry().getAsJsonObject(geometryName).getAsJsonArray("bones")) {
                    bones.add(node.getAsJsonObject());
                }
            } else { // Seen with format_version 1.12.0
                for (JsonElement node : info.getGeometry().getAsJsonArray("minecraft:geometry")) {
                    JsonObject o = node.getAsJsonObject();
                    JsonObject description = o.get("description").getAsJsonObject();
                    if (!description.get("identifier").getAsString().equals(geometryName)) {
                        continue;
                    } else {
                        uvHeight = description.get("texture_height").getAsInt();
                        uvWidth = description.get("texture_width").getAsInt();
                    }
                    for (JsonElement subNode : o.get("bones").getAsJsonArray()) {
                        bones.add(subNode.getAsJsonObject());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while parsing geometry!");
            e.printStackTrace();
            return null;
        }

        Map<String, PartInfo> stringToPart = new HashMap<>();
        try {
            for (JsonObject bone : bones) {
                // Iterate through all bones
                String name = bone.get("name").getAsString();

                JsonElement jsonParent = bone.get("parent");
                JsonObject parentPart = null;
                String parent = null;
                if (jsonParent != null) {
                    // Search through all bones to find the parent part
                    parent = jsonParent.getAsString();
                    for (JsonObject otherBone : bones) {
                        if (parent.equals(otherBone.get("name").getAsString())) {
                            parentPart = otherBone;
                            break;
                        }
                    }
                }

                List<ModelPart.Cube> cuboids = new ArrayList<>();
                JsonArray pivot = bone.getAsJsonArray("pivot");
                float pivotX = pivot.get(0).getAsFloat();
                float pivotY = pivot.get(1).getAsFloat();
                float pivotZ = pivot.get(2).getAsFloat();

                JsonArray cubes = bone.getAsJsonArray("cubes");
                if (cubes != null) {
                    for (JsonElement node : cubes) {
                        JsonObject cube = node.getAsJsonObject();
                        JsonElement mirrorNode = cube.get("mirror"); // Can be null on the llama skins in the Wandering Trader pack
                        boolean mirrored = mirrorNode != null && mirrorNode.getAsBoolean();
                        JsonArray origin = cube.getAsJsonArray("origin");
                        float originX = origin.get(0).getAsFloat();
                        float originY = origin.get(1).getAsFloat();
                        float originZ = origin.get(2).getAsFloat();
                        JsonArray size = cube.getAsJsonArray("size");
                        float sizeX = size.get(0).getAsFloat();
                        float sizeY = size.get(1).getAsFloat();
                        float sizeZ = size.get(2).getAsFloat();
                        JsonArray uv = cube.getAsJsonArray("uv");
                        JsonElement inflateNode = cube.get("inflate"); // Again, the llama skin
                        float inflate = inflateNode != null ? inflateNode.getAsFloat() : 0f;
                        // I didn't use the below, but it may be a helpful reference in the future
                        // The Y needs to be inverted, for whatever reason
                        // https://github.com/JannisX11/blockbench/blob/8529c0adee8565f8dac4b4583c3473b60679966d/js/transform.js#L148
                        cuboids.add(new ModelPart.Cube((int) uv.get(0).getAsFloat(), (int) uv.get(1).getAsFloat(),
                                (originX - pivotX), (-(originY + sizeY) + pivotY), (originZ - pivotZ),
                                sizeX, sizeY, sizeZ, inflate, inflate, inflate, mirrored, uvHeight, uvWidth, ALL_VISIBLE));
                    }
                }

                Map<String, ModelPart> children = new HashMap<>();
                ModelPart part = new ModelPart(cuboids, children);
                if (parentPart != null) {
                    // This appears to be a difference between Bedrock and Java - pivots are carried over for us
                    JsonArray parentPivot = parentPart.getAsJsonArray("pivot");
                    part.setPos(pivotX - parentPivot.get(0).getAsFloat(),
                            pivotY - parentPivot.get(1).getAsFloat(),
                            pivotZ - parentPivot.get(2).getAsFloat());
                } else {
                    part.setPos(pivotX, pivotY, pivotZ);
                }

                switch (name) { // Also do this with the overlays? Those are final, though.
                    case "head", "hat", "rightArm", "body", "leftArm", "leftLeg", "rightLeg" -> parent = "root";
                }

                name = adjustFormatting(name);

                stringToPart.put(name, new PartInfo(adjustFormatting(parent), part, children));
            }

            for (Map.Entry<String, PartInfo> entry : stringToPart.entrySet()) {
                if (entry.getValue().parent != null) {
                    PartInfo parentPart = stringToPart.get(entry.getValue().parent);
                    if (parentPart != null) {
                        parentPart.children.put(entry.getKey(), entry.getValue().part);
                    }
                }
            }

            PartInfo root = stringToPart.get("root");

            ensureAvailable(root.children, "ear");
            root.children.computeIfAbsent("cloak", (string) -> // Required to allow a cape to render
                            HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F).getRoot().addOrReplaceChild(string,
                                    CubeListBuilder.create()
                                            .texOffs(0, 0)
                                            .addBox(-5.0F, 0.0F, -1.0F, 10.0F, 16.0F, 1.0F, CubeDeformation.NONE, 1.0F, 0.5F),
                                    PartPose.offset(0.0F, 0.0F, 0.0F)).bake(64, 64));
            ensureAvailable(root.children, "left_sleeve");
            ensureAvailable(root.children, "right_sleeve");
            ensureAvailable(root.children, "left_pants");
            ensureAvailable(root.children, "right_pants");
            ensureAvailable(root.children, "jacket");

            // Create base model
            return new BedrockPlayerEntityModel<>(root.part);
        } catch (Exception e) {
            LOGGER.error("Error while parsing geometry into model!", e);
            return null;
        }
    }

    private static String adjustFormatting(String name) {
        if (name == null) {
            return null;
        }

        return switch (name) {
            case "leftArm" -> "left_arm";
            case "rightArm" -> "right_arm";
            case "leftLeg" -> "left_leg";
            case "rightLeg" -> "right_leg";
            default -> name;
        };
    }

    /**
     * Ensure a part is created, or else the geometry will not load in 1.17.
     */
    private static void ensureAvailable(Map<String, ModelPart> children, String name) {
        children.computeIfAbsent(name, (string) -> new ModelPart(Collections.emptyList(), Maps.newHashMap()));
    }

    private record PartInfo(String parent, ModelPart part, Map<String, ModelPart> children) {
    }
}
