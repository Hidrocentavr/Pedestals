package com.mowmaster.pedestals.crafting;

import com.mowmaster.pedestals.item.ItemColorPallet;
import com.mowmaster.pedestals.item.ItemLinkingTool;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.ExplosionContext;
//import net.minecraft.world.IExplosionContext;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;


@Mod.EventBusSubscriber
public class ColorPalletCrafting
{
    @SubscribeEvent()
    public static void SpellCrafting(PlayerInteractEvent.RightClickBlock event)
    {
        //Added to keep fake players from canning this every time?
        if(!(event.getPlayer() instanceof FakePlayer))
        {
            World worldIn = event.getWorld();
            Hand hand = event.getHand();
            BlockState state = worldIn.getBlockState(event.getPos());
            PlayerEntity player = event.getPlayer();
            BlockPos pos = event.getPos();

            int posX = event.getPos().getX();
            int posY = event.getPos().getY();
            int posZ = event.getPos().getZ();

            double r = 0;
            double g = 0;
            double b = 0;
            double red=0;
            double blue=0;
            double green=0;
            int white=0;
            int black=0;

            double cr = 0;
            double cg = 0;
            double cb = 0;
            double cred=0;
            double cblue=0;
            double cgreen=0;

            int pallet=0;
            ItemStack trapped = ItemStack.EMPTY;

            if(!worldIn.isRemote) {
                ItemStack coined = ItemStack.EMPTY;
                if ((player.getHeldItem(hand) != null)) {
                    if (player.getHeldItem(hand).getItem() instanceof ItemLinkingTool) {
                        //List<EntityItem> item = player.getEntityWorld().getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(posX-1, posY-1, posZ-1, posX+1, posY+1, posZ+1));
                        List<ItemEntity> items = player.getEntityWorld().getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB(posX - 3, posY - 3, posZ - 3, posX + 3, posY + 3, posZ + 3));

                        for (ItemEntity item : items) {
                            ItemStack stack = item.getItem();

                            if(stack.getItem() instanceof ItemColorPallet)
                            {
                                pallet +=stack.getCount();
                                item.remove();
                            }

                            //Thanks to https://www.twitch.tv/willywonka2197 for realizing i didnt have this TAGed yet;
                            ResourceLocation grabRed = new ResourceLocation("forge", "dyes/red");
                            ResourceLocation grabGreen = new ResourceLocation("forge", "dyes/green");
                            ResourceLocation grabBlue = new ResourceLocation("forge", "dyes/blue");
                        /*ITag.INamedTag<Item> RED_DYE = ItemTags.createOptional(grabRed);
                        ITag.INamedTag<Item> GREEN_DYE = ItemTags.createOptional(grabGreen);
                        ITag.INamedTag<Item> BLUE_DYE = ItemTags.createOptional(grabBlue);*/
                            ITag<Item> RED_DYE = ItemTags.getCollection().get(grabRed);
                            ITag<Item> GREEN_DYE = ItemTags.getCollection().get(grabGreen);
                            ITag<Item> BLUE_DYE = ItemTags.getCollection().get(grabBlue);

                            if(stack.getItem() instanceof DyeItem)
                            {

                                if(RED_DYE.contains(stack.getItem()))
                                {
                                    //16711680
                                    double[] rgbColors = CalculateColor.getRGBColorFromIntCount(16711680,stack.getCount());
                                    r+=rgbColors[0];
                                    g+=rgbColors[1];
                                    b+=rgbColors[2];
                                    item.remove();
                                }
                                else if(GREEN_DYE.contains(stack.getItem()))
                                {
                                    //65280
                                    double[] rgbColors = CalculateColor.getRGBColorFromIntCount(65280,stack.getCount());
                                    r+=rgbColors[0];
                                    g+=rgbColors[1];
                                    b+=rgbColors[2];
                                    item.remove();
                                }
                                else if(BLUE_DYE.contains(stack.getItem()))
                                {
                                    //255
                                    double[] rgbColors = CalculateColor.getRGBColorFromIntCount(255,stack.getCount());
                                    r+=rgbColors[0];
                                    g+=rgbColors[1];
                                    b+=rgbColors[2];
                                    item.remove();
                                }
                            }
                        }


                        if(pallet > 0)
                        {
                            red = r%256;
                            green = g%256;
                            blue = b%256;

                            double rgbRed = red;
                            double rgbGreen = green;
                            double rgbBlue = blue;

                            int color = CalculateColor.getColorFromRGB(rgbRed,rgbGreen,rgbBlue);

                            //removes fire block???
                            //worldIn.removeBlock(new BlockPos(posX, posY + 1, posZ), false);
                            //worldIn.createExplosion(new ItemEntity(worldIn, posX, posY, posZ),(DamageSource)null,(IExplosionContext)null, posX + 0.5, posY + 2.0, posZ + 0.25, 0.0F,false, Explosion.Mode.NONE);
                            worldIn.createExplosion(new ItemEntity(worldIn, posX, posY, posZ),(DamageSource)null,(ExplosionContext)null, posX + 0.5, posY + 2.0, posZ + 0.25, 0.0F,false, Explosion.Mode.NONE);
                            if(pallet>0)
                            {
                                //NEED TO ADD ANOTHER TAG TO ITEM TO MAKE IT NOT USEABLE IN COMBINING AGAIN!!!
                                ItemStack stacked = new ItemStack(ItemColorPallet.COLORPALLET,1);
                                CompoundNBT nbt = new CompoundNBT();
                                nbt.putInt("color",color);
                                if(color == 255 || color == 16711680 || color == 65280) {} else {nbt.putBoolean("combine",false);}
                                stacked.setTag(nbt);
                                stacked.setCount(pallet);
                                ItemEntity itemEn = new ItemEntity(worldIn,posX,posY+1,posZ,stacked);
                                itemEn.setInvulnerable(true);
                                worldIn.addEntity(itemEn);
                            }
                        }
                    }
                }
            }
        }
    }
}
