package me.paypur.tconjei.jei;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoJson;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.tools.stats.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static me.paypur.tconjei.TConJEI.MOD_ID;
import static net.minecraftforge.common.ForgeI18n.getPattern;

public class MaterialStatsCategory implements IRecipeCategory<MaterialStatsWrapper> {

    final ResourceLocation UID = new ResourceLocation(MOD_ID, "material_stats");
    final Font font = Minecraft.getInstance().font;
    final IDrawable BACKGROUND, ICON;
    final int WIDTH = 164, HEIGHT = 240;
    final int LINE_OFFSET = 20;
    final int LINE_OFFSET_HOVER = LINE_OFFSET - 1;
    final int LINE_HEIGHT = 10;
    final int WHITE = 16777215;
    int PRIMARY_COLOR = 0;
    int SECOND_COLOR = 8289918;
    int DURABILITY_COLOR = 4639302;

    public MaterialStatsCategory(IGuiHelper guiHelper) {
        this.BACKGROUND = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.ICON = guiHelper.createDrawable(new ResourceLocation(MOD_ID, "textures/gui/icon.png"), 0, 0, 16, 16);
        ResourceLocation palette = new ResourceLocation(MOD_ID, "textures/gui/palette.png");
        try {
            InputStream stream = Minecraft.getInstance().getResourceManager().getResource(palette).getInputStream();
            BufferedImage image = ImageIO.read(stream);
            this.PRIMARY_COLOR = image.getRGB(0, 0);
            this.SECOND_COLOR = image.getRGB(1, 0);
        } catch (IOException e) {
            LogUtils.getLogger().error("Error loading palette", e);
        }
    }

    @Override
    public void draw(MaterialStatsWrapper statsWrapper, IRecipeSlotsView recipeSlotsView, PoseStack poseStack, double mouseX, double mouseY) {
        final int MATERIAL_COLOR = MaterialTooltipCache.getColor(statsWrapper.getMaterialId()).getValue();

        final String materialName = getPattern(String.format("material.%s.%s", statsWrapper.getMaterialId().getNamespace(), statsWrapper.getMaterialId().getPath()));
        float lineNumber = 0;

        font.drawShadow(poseStack, materialName, (WIDTH - font.getSplitter().stringWidth(materialName)) / 2, 4, MATERIAL_COLOR);

        Optional<HeadMaterialStats> headStats = statsWrapper.getStats(HeadMaterialStats.ID);
        Optional<ExtraMaterialStats> extraStats = statsWrapper.getStats(ExtraMaterialStats.ID);
        Optional<HandleMaterialStats> handleStats = statsWrapper.getStats(HandleMaterialStats.ID);
        Optional<LimbMaterialStats> limbStats = statsWrapper.getStats(LimbMaterialStats.ID);
        Optional<GripMaterialStats> gripStats = statsWrapper.getStats(GripMaterialStats.ID);
        Optional<BowstringMaterialStats> stringStats = statsWrapper.getStats(BowstringMaterialStats.ID);

        if (headStats.isPresent()) {
            String miningLevel = headStats.get().getTierId().getPath();
            drawTraits(poseStack, statsWrapper, HeadMaterialStats.ID, lineNumber);
            font.drawShadow(poseStack, String.format("[%s]", getPattern("stat.tconstruct.head")), 0, lineNumber++ * LINE_HEIGHT + LINE_OFFSET, PRIMARY_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.durability", "%d", headStats.get().getDurability(), lineNumber++, DURABILITY_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.harvest_tier", "%s", getPattern("stat.tconstruct.harvest_tier.minecraft." + miningLevel), lineNumber++, getMiningLevelColor(miningLevel));
            drawStatsColor(poseStack, "tool_stat.tconstruct.mining_speed", "%.2f", headStats.get().getMiningSpeed(), lineNumber++, SECOND_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.attack_damage", "%.2f", headStats.get().getAttack(), lineNumber++, SECOND_COLOR);
            lineNumber += 0.5f;
        }

        if (extraStats.isPresent()) {
            drawTraits(poseStack, statsWrapper, ExtraMaterialStats.ID, lineNumber);
            font.drawShadow(poseStack, String.format("[%s]", getPattern("stat.tconstruct.extra")), 0, lineNumber++ * LINE_HEIGHT + LINE_OFFSET, PRIMARY_COLOR);
            lineNumber += 0.5f;
        }

        if (handleStats.isPresent()) {
            drawTraits(poseStack, statsWrapper, HandleMaterialStats.ID, lineNumber);
            font.drawShadow(poseStack, String.format("[%s]", getPattern("stat.tconstruct.handle")), 0, lineNumber++ * LINE_HEIGHT + LINE_OFFSET, PRIMARY_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.durability", "%.2fx", handleStats.get().getDurability(), lineNumber++, DURABILITY_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.attack_damage", "%.2fx", handleStats.get().getAttackDamage(), lineNumber++, SECOND_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.attack_speed", "%.2fx", handleStats.get().getAttackSpeed(), lineNumber++, SECOND_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.mining_speed", "%.2fx", handleStats.get().getMiningSpeed(), lineNumber++, SECOND_COLOR);
            lineNumber += 0.5f;
        }

        if (limbStats.isPresent()) {
            drawTraits(poseStack, statsWrapper, LimbMaterialStats.ID, lineNumber);
            font.drawShadow(poseStack, String.format("[%s]", getPattern("stat.tconstruct.limb")), 0, lineNumber++ * LINE_HEIGHT + LINE_OFFSET, PRIMARY_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.durability", "%d", limbStats.get().getDurability(), lineNumber++, DURABILITY_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.draw_speed", "%s",  signedString(limbStats.get().getDrawSpeed()), lineNumber++, SECOND_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.velocity", "%s", signedString(limbStats.get().getVelocity()), lineNumber++, SECOND_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.accuracy", "%s", signedString(limbStats.get().getAccuracy()), lineNumber++, SECOND_COLOR);
            lineNumber += 0.5f;
        }

        if (gripStats.isPresent()) {
            drawTraits(poseStack, statsWrapper, GripMaterialStats.ID, lineNumber);
            font.drawShadow(poseStack, String.format("[%s]", getPattern("stat.tconstruct.grip")), 0, lineNumber++ * LINE_HEIGHT + LINE_OFFSET, PRIMARY_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.durability", "%.2fx", gripStats.get().getDurability(), lineNumber++, DURABILITY_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.accuracy", "%s", signedString(gripStats.get().getAccuracy()), lineNumber++, SECOND_COLOR);
            drawStatsColor(poseStack, "tool_stat.tconstruct.attack_damage", "%.2f", gripStats.get().getMeleeAttack(), lineNumber++, SECOND_COLOR);
            lineNumber += 0.5f;
        }

        if (stringStats.isPresent()) {
            drawTraits(poseStack, statsWrapper, BowstringMaterialStats.ID, lineNumber);
            font.drawShadow(poseStack, String.format("[%s]", getPattern("stat.tconstruct.bowstring")), 0, lineNumber * LINE_HEIGHT + LINE_OFFSET, PRIMARY_COLOR);
        }

    }
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MaterialStatsWrapper recipe, IFocusGroup focuses) {
        if (!recipe.isCraftable()) {
            final int BUCKET = 1000; // milli buckets
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 18, 0).addFluidStack(recipe.getFluidStack().getFluid(), BUCKET);
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addFluidStack(recipe.getFluidStack().getFluid(), BUCKET);
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addFluidStack(recipe.getFluidStack().getFluid(), BUCKET);
        }
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 0, 0).addItemStacks(recipe.getItemStacks());
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(recipe.getItemStacks());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(recipe.getItemStacks());

        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, (WIDTH - 16), 0).addItemStacks(recipe.getParts());
    }
    @Override
    public List<Component> getTooltipStrings(MaterialStatsWrapper statsWrapper, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        final String materialNamespace = statsWrapper.getMaterialId().getNamespace();
        final String materialPath = statsWrapper.getMaterialId().getPath();

        float lineNumber = 0;

        int matWidth = font.width(materialPath);
        if (inBox(mouseX, mouseY, (WIDTH - matWidth) / 2, 3, matWidth, LINE_HEIGHT)) {
            return Collections.singletonList(new TranslatableComponent(String.format("material.%s.%s.flavor", materialNamespace, materialPath)).setStyle(Style.EMPTY.withItalic(true).withColor(WHITE)));
        }

        Optional<HeadMaterialStats> headStats = MaterialRegistry.getInstance().getMaterialStats(statsWrapper.getMaterialId(), HeadMaterialStats.ID);
        Optional<ExtraMaterialStats> extraStats = MaterialRegistry.getInstance().getMaterialStats(statsWrapper.getMaterialId(), ExtraMaterialStats.ID);
        Optional<HandleMaterialStats> handleStats = MaterialRegistry.getInstance().getMaterialStats(statsWrapper.getMaterialId(), HandleMaterialStats.ID);
        Optional<LimbMaterialStats> limbStats = MaterialRegistry.getInstance().getMaterialStats(statsWrapper.getMaterialId(), LimbMaterialStats.ID);
        Optional<GripMaterialStats> gripStats = MaterialRegistry.getInstance().getMaterialStats(statsWrapper.getMaterialId(), GripMaterialStats.ID);
        Optional<BowstringMaterialStats> stringStats = MaterialRegistry.getInstance().getMaterialStats(statsWrapper.getMaterialId(), BowstringMaterialStats.ID);

        List<Component> components = Collections.emptyList();

        if (headStats.isPresent()) {
            components = getTraitTooltips(statsWrapper, BowstringMaterialStats.ID, mouseX, mouseY, lineNumber);
            if (!components.isEmpty()) {
                return components;
            }
            lineNumber += 5.5f;
        }

        if (extraStats.isPresent()) {
            components = getTraitTooltips(statsWrapper, BowstringMaterialStats.ID, mouseX, mouseY, lineNumber);
            if (!components.isEmpty()) {
                return components;
            }
            lineNumber += 1.5f;
        }

        if (handleStats.isPresent()) {
            components = getTraitTooltips(statsWrapper, BowstringMaterialStats.ID, mouseX, mouseY, lineNumber);
            if (!components.isEmpty()) {
                return components;
            }
            lineNumber += 5.5f;
        }

        if (limbStats.isPresent()) {
            components = getTraitTooltips(statsWrapper, BowstringMaterialStats.ID, mouseX, mouseY, lineNumber);
            if (!components.isEmpty()) {
                return components;
            }
            lineNumber += 5.5f;
        }

        if (gripStats.isPresent()) {
            components = getTraitTooltips(statsWrapper, BowstringMaterialStats.ID, mouseX, mouseY, lineNumber);
            if (!components.isEmpty()) {
                return components;
            }
            lineNumber += 4.5f;
        }

        if (stringStats.isPresent()) {
            components = getTraitTooltips(statsWrapper, BowstringMaterialStats.ID, mouseX, mouseY, lineNumber);
            if (!components.isEmpty()) {
                return components;
            }
        }

        return components;
    }
    private <T> void drawStatsColor(PoseStack poseStack, String type, String statFormat, T stat, float lineNumber, int ACCENT_COLOR) {
        String pattern = getPattern(type);
        float width = font.getSplitter().stringWidth(pattern);
        font.draw(poseStack, pattern, 0, lineNumber * LINE_HEIGHT + LINE_OFFSET, SECOND_COLOR);
        font.draw(poseStack, String.format(statFormat, stat), width, lineNumber * LINE_HEIGHT + LINE_OFFSET, ACCENT_COLOR);
    }

    private int getMiningLevelColor(String miningLevel) {
        return switch (miningLevel) {
            case "wood" -> 9200923;
            case "stone" -> 9934743;
            case "iron" -> 13158600;
            case "diamond" -> 5569788;
            case "netherite" -> 4997443;
            default -> SECOND_COLOR;
        };
    }

    private String signedString(float f) {
        return String.format("%s%.2f", f >= 0 ? "+" : "", f);
    }

    private void drawTraits(PoseStack poseStack, MaterialStatsWrapper statsWrapper, MaterialStatsId materialStatsId, float lineNumber) {
        for (ModifierEntry trait : statsWrapper.getTraits(materialStatsId)) {
            String pattern = getPattern(String.format("modifier.%s.%s", trait.getId().getNamespace(), trait.getId().getPath()));
            font.draw(poseStack, String.format("%s", pattern), WIDTH - font.getSplitter().stringWidth(pattern), lineNumber++ * LINE_HEIGHT + LINE_OFFSET, SECOND_COLOR);
        }
    }

    private List<Component> getTraitTooltips(MaterialStatsWrapper statsWrapper, MaterialStatsId materialStatsId, double mouseX, double mouseY, float lineNumber) {
        List<ModifierEntry> traits = statsWrapper.getTraits(materialStatsId);
        for (ModifierEntry trait : traits) {
            String namespace = trait.getId().getNamespace();
            String path = trait.getId().getPath();
            String pattern = getPattern(String.format("modifier.%s.%s", namespace, path));
            int textWidth = font.width(pattern);
            if (inBox(mouseX, mouseY, WIDTH - textWidth, (int) (lineNumber * LINE_HEIGHT + LINE_OFFSET_HOVER), textWidth, LINE_HEIGHT)) {
                return List.of(new TranslatableComponent(String.format("modifier.%s.%s.flavor", namespace, path)).setStyle(Style.EMPTY.withItalic(true).withColor(WHITE)),
                        new TranslatableComponent(String.format("modifier.%s.%s.description", namespace, path)));
            }
            lineNumber += 1f;
        }
        return Collections.emptyList();
    }

    private boolean inBox(double mX, double mY, int x, int y, int w, int h) {
        return (x <= mX && mX <= x + w && y <= mY && mY <= y + h);
    }

    @Override
    public IDrawable getBackground() {
        return BACKGROUND;
    }

    @Override
    public IDrawable getIcon() {
        return ICON;
    }

    @Override
    public Component getTitle() {
        return new TextComponent("Material Stats");
    }

    @SuppressWarnings("removal")
    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @SuppressWarnings("removal")
    @Override
    public Class<? extends MaterialStatsWrapper> getRecipeClass() {
        return MaterialStatsWrapper.class;
    }

    @Override
    public RecipeType<MaterialStatsWrapper> getRecipeType() {
        return RecipeType.create(MOD_ID, "material_stats", getRecipeClass());
    }

}
