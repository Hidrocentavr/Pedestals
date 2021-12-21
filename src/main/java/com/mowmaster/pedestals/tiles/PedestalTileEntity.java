package com.mowmaster.pedestals.tiles;

import com.mowmaster.pedestals.api.filter.IFilterBase;
import com.mowmaster.pedestals.api.upgrade.IUpgradeBase;
import com.mowmaster.pedestals.blocks.PedestalBlock;
import com.mowmaster.pedestals.crafting.CraftingPedestals;
import com.mowmaster.pedestals.item.augments.ItemPedestalRenderAugment;
import com.mowmaster.pedestals.item.augments.ItemPedestalUpgrades;
import com.mowmaster.pedestals.item.pedestalFilters.ItemFilterBase;
import com.mowmaster.pedestals.item.pedestalUpgrades.*;
import com.mowmaster.pedestals.network.PacketHandler;
import com.mowmaster.pedestals.network.PacketParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.*;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LockCode;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.mowmaster.pedestals.references.Reference.MODID;
//Idk if the implement is needed??? IF so then have to uncomment the other energy methods
//public class PedestalTileEntity extends TileEntity implements ITickableTileEntity, IEnergyStorage {
public class PedestalTileEntity extends TileEntity implements ITickableTileEntity {

    private LazyOptional<IItemHandler> handler = LazyOptional.of(this::createHandler);
    private LazyOptional<IItemHandler> privateHandler = LazyOptional.of(this::createHandlerPedestalPrivate);
    private LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(this::createHandlerEnergy);
    private LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(this::createHandlerFluid);
    private List<ItemStack> stacksList = new ArrayList<>();

    private static final int[] SLOTS_ALLSIDES = new int[] {0};

    private int storedValueForUpgrades = 0;
    private boolean boolLight = false;
    private final List<BlockPos> storedLocations = new ArrayList<BlockPos>();
    private LockCode lockCode;

    public PedestalTileEntity()
    {
        super(PEDESTALTYPE);
        this.lockCode = LockCode.EMPTY_CODE;
    }

    /**********************************
     **********************************
     **  ENERGY IMPLIMENTION START  ***
     **********************************
     *********************************/

    /*
    For eventual conversion to handlers within the pedestal and not using upgrades to transfer energy/fluids/xp...
    Base transfer speed needs to be 5000/s (250/t)
    With storage of 10k base
     */

    public IEnergyStorage createHandlerEnergy() {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                if((hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeEnergyImport))
                {
                    ItemUpgradeBaseEnergy itemE =  (ItemUpgradeBaseEnergy)getCoinOnPedestal().getItem();
                    return itemE.fillEnergy(getCoinOnPedestal(),maxReceive,simulate);

                    /*if(itemE.addEnergy(getCoinOnPedestal(),maxReceive,simulate))
                    {
                        //Return input power if it can be insert???
                        return Math.min(maxReceive,itemE.availableEnergySpaceInCoin(getCoinOnPedestal()));
                    }
                    //Else return availablespace???
                    else return itemE.availableEnergySpaceInCoin(getCoinOnPedestal());*/
                }
                else return 0;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                if((hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeEnergyExport))
                {
                    ItemUpgradeBaseEnergy itemE =  (ItemUpgradeBaseEnergy)getCoinOnPedestal().getItem();
                    if(itemE.removeEnergy(getCoinOnPedestal(),maxExtract,simulate))
                    {
                        //Return output power if it can be taken???
                        return maxExtract;
                    }
                    //Else return currently stored energy???
                    else return itemE.getEnergyStored(getCoinOnPedestal());
                }
                return 0;
            }

            @Override
            public int getEnergyStored() {
                if(hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeBaseEnergy)
                {
                    ItemUpgradeBaseEnergy itemE =  (ItemUpgradeBaseEnergy)getCoinOnPedestal().getItem();
                    return itemE.getEnergyStored(getCoinOnPedestal());
                }
                else return 0;
            }

            @Override
            public int getMaxEnergyStored() {
                if(hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeBaseEnergy)
                {
                    ItemUpgradeBaseEnergy itemE =  (ItemUpgradeBaseEnergy)getCoinOnPedestal().getItem();
                    return itemE.getEnergyBuffer(getCoinOnPedestal());
                }
                else return 0;
            }

            @Override
            public boolean canExtract() {
                return (hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeEnergyExport);
            }

            @Override
            public boolean canReceive() {
                return (hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeEnergyImport);
            }
        };
    }

    /*@Override
    public int receiveEnergy(int maxReceive, boolean simulate) {

        if((hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeEnergyImport))
        {
            ItemUpgradeBaseEnergy itemE =  (ItemUpgradeBaseEnergy)getCoinOnPedestal().getItem();
            if(itemE.addEnergy(getCoinOnPedestal(),maxReceive,simulate))
            {
                //Return input power if it can be insert???
                return maxReceive;
            }
            //Else return availablespace???
            else return itemE.availableEnergySpaceInCoin(getCoinOnPedestal());
        }
        *//*if((hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeEnergyImport))
        {
            ItemUpgradeBaseEnergy itemE =  (ItemUpgradeBaseEnergy)getCoinOnPedestal().getItem();
            if(itemE.addEnergy(getCoinOnPedestal(),maxReceive,simulate))
            {
                //Return input power if it can be insert???
                return maxReceive;
            }
        }*//*

        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        *//*if((hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeEnergyExport))
        {
            ItemUpgradeBaseEnergy itemE =  (ItemUpgradeBaseEnergy)getCoinOnPedestal().getItem();
            if(itemE.removeEnergy(getCoinOnPedestal(),maxExtract,simulate))
            {
                //Return output power if it can be taken???
                return maxExtract;
            }
        }
        return 0;*//*
        if((hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeEnergyExport))
        {
            ItemUpgradeBaseEnergy itemE =  (ItemUpgradeBaseEnergy)getCoinOnPedestal().getItem();
            if(itemE.removeEnergy(getCoinOnPedestal(),maxExtract,simulate))
            {
                //Return output power if it can be taken???
                return maxExtract;
            }
            //Else return currently stored energy???
            else return itemE.getEnergyStored(getCoinOnPedestal());
        }
        return 0;
    }

    @Override
    public int getEnergyStored() {
        if(hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeBaseEnergy)
        {
            ItemUpgradeBaseEnergy itemE =  (ItemUpgradeBaseEnergy)getCoinOnPedestal().getItem();
            return itemE.getEnergyStored(getCoinOnPedestal());
        }
        else return 0;
    }

    @Override
    public int getMaxEnergyStored() {
        if(hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeBaseEnergy)
        {
            ItemUpgradeBaseEnergy itemE =  (ItemUpgradeBaseEnergy)getCoinOnPedestal().getItem();
            return itemE.getEnergyBuffer(getCoinOnPedestal());
        }
        else return 0;
    }

    @Override
    public boolean canExtract() {
        return (hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeEnergyExport);
    }

    @Override
    public boolean canReceive() {
        return (hasCoin() && getCoinOnPedestal().getItem() instanceof ItemUpgradeEnergyImport);
    }

    public int getMaxEnergyReceive()
    {
        int est = this.getMaxEnergyStored() - this.getEnergyStored();
        return (est > 0)?(est):(0);
    }

    public int getMaxEnergyExtract()
    {
        return this.getEnergyStored();
    }*/



    /**********************************
     **********************************
     ***  ENERGY IMPLIMENTION END   ***
     **********************************
     *********************************/

    /**********************************
     **********************************
     **  Fluid IMPLIMENTION START  ***
     **********************************
     *********************************/

    public IFluidHandler createHandlerFluid() {
        return new IFluidHandler() {
            @Nonnull
            @Override
            public FluidStack getFluidInTank(int i) {
                ItemStack coin = getCoinOnPedestal();
                return (coin.getItem() instanceof ItemUpgradeBaseFluid)?(((ItemUpgradeBaseFluid)coin.getItem()).getFluidStored(coin)):(FluidStack.EMPTY);
            }

            @Override
            public int getTanks() {
                //Technically we dont use the tanks thing, but we'll pretend and hope it doesnt break...
                return 1;
            }

            @Nonnull
            @Override
            public FluidStack drain(int i, FluidAction fluidAction) {
                ItemStack coin = getCoinOnPedestal();
                if(coin.getItem() instanceof ItemUpgradeFluidExport)
                {
                    ItemUpgradeFluidExport coinFluid = ((ItemUpgradeFluidExport)coin.getItem());
                    FluidStack coinFluidStored = coinFluid.getFluidStored(coin);
                    int storedCoinFluidSpace = coinFluid.availableFluidSpaceInCoin(coin);
                    int getMainTransferRate = coinFluid.getFluidTransferRate(coin);
                    int transferRate = (getMainTransferRate <= storedCoinFluidSpace)?(getMainTransferRate):(storedCoinFluidSpace);
                    FluidStack fluidToDrain = new FluidStack(coinFluidStored.getFluid(),transferRate,coinFluidStored.getTag());
                    if(coinFluid.hasFluidInCoin(coin))
                    {
                        if(fluidToDrain.getAmount() > 0)
                        {
                            if(fluidAction != FluidAction.SIMULATE)
                            {
                                if(coinFluid.removeFluid(getTile(),coin,fluidToDrain,true))
                                {
                                    coinFluid.removeFluid(getTile(),coin,fluidToDrain,false);
                                    return fluidToDrain;
                                }
                                else return FluidStack.EMPTY;
                            }
                            else return fluidToDrain;
                        }
                    }
                }
                return FluidStack.EMPTY;
            }

            @Nonnull
            @Override
            public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
                ItemStack coin = getCoinOnPedestal();
                if(coin.getItem() instanceof ItemUpgradeFluidExport)
                {
                    ItemUpgradeFluidExport coinFluid = ((ItemUpgradeFluidExport)coin.getItem());
                    FluidStack coinFluidStored = coinFluid.getFluidStored(coin);
                    int storedCoinFluidSpace = coinFluid.availableFluidSpaceInCoin(coin);
                    int getMainTransferRate = coinFluid.getFluidTransferRate(coin);
                    int transferRate = (getMainTransferRate <= storedCoinFluidSpace)?(getMainTransferRate):(storedCoinFluidSpace);
                    FluidStack fluidToDrain = new FluidStack(coinFluidStored.getFluid(),transferRate,coinFluidStored.getTag());
                    if(coinFluid.hasFluidInCoin(coin))
                    {
                        if(fluidToDrain.getAmount() > 0)
                        {
                            if(fluidAction != FluidAction.SIMULATE)
                            {
                                if(coinFluid.removeFluid(getTile(),coin,fluidToDrain,true))
                                {
                                    coinFluid.removeFluid(getTile(),coin,fluidToDrain,false);
                                    return fluidToDrain;
                                }
                                else return FluidStack.EMPTY;
                            }
                            else return fluidToDrain;
                        }
                    }
                }
                return FluidStack.EMPTY;
            }

            @Override
            public int getTankCapacity(int i) {
                ItemStack coin = getCoinOnPedestal();
                return (coin.getItem() instanceof ItemUpgradeBaseFluid)?(((ItemUpgradeBaseFluid)coin.getItem()).getFluidbuffer(coin)):(0);
            }

            @Override
            public boolean isFluidValid(int i, @Nonnull FluidStack fluidStack) {
                ItemStack coin = getCoinOnPedestal();
                if(coin.getItem() instanceof ItemUpgradeBaseFluid)
                {
                    FluidStack inCoin = ((ItemUpgradeBaseFluid) coin.getItem()).getFluidStored(coin);
                    return inCoin.isFluidEqual(fluidStack)  || inCoin.isEmpty() && ((ItemUpgradeBaseFluid) coin.getItem()).canRecieveFluid(getWorld(), getPos(), fluidStack);
                }

                return false;
            }

            @Override
            public int fill(FluidStack fluidStack, FluidAction fluidAction) {
                ItemStack coin = getCoinOnPedestal();
                if(coin.getItem() instanceof ItemUpgradeFluidImport)
                {
                    ItemUpgradeFluidImport coinFluid = ((ItemUpgradeFluidImport)coin.getItem());
                    FluidStack coinFluidStored = coinFluid.getFluidStored(coin);
                    int storedCoinFluidSpace = coinFluid.availableFluidSpaceInCoin(coin);
                    int getMainTransferRate = coinFluid.getFluidTransferRate(coin);
                    int transferRate = (getMainTransferRate <= storedCoinFluidSpace)?(getMainTransferRate):(storedCoinFluidSpace);
                    if(coinFluidStored.isFluidEqual(fluidStack) || coinFluidStored.isEmpty() && coinFluid.canRecieveFluid(getWorld(), getPos(), fluidStack))
                    {
                        if(storedCoinFluidSpace > 0)
                        {
                            if(fluidAction != FluidAction.SIMULATE)
                            {
                                FluidStack fluidToStore = new FluidStack(fluidStack.getFluid(),transferRate,fluidStack.getTag());
                                if(coinFluid.addFluid(getTile(),coin,fluidToStore,true))
                                {
                                    coinFluid.addFluid(getTile(),coin,fluidToStore,false);
                                    return fluidToStore.getAmount();
                                }
                                else return 0;
                            }
                            else return transferRate;
                        }
                    }
                }
                return 0;
            }
        };
    }

    /**********************************
     **********************************
     ***  Fluid IMPLIMENTION END   ***
     **********************************
     *********************************/



    public void update()
    {
        this.markDirty();
        this.world.notifyBlockUpdate(pos,getBlockState(),getBlockState(),2);
        //do this for pedestals that do inv buffers
        this.world.notifyBlockUpdate(pos,getBlockState(),getBlockState(),1);
        /*public static final int NOTIFY_NEIGHBORS = 1;
        public static final int BLOCK_UPDATE = 2;
        public static final int NO_RERENDER = 4;
        public static final int RERENDER_MAIN_THREAD = 8;
        public static final int UPDATE_NEIGHBORS = 16;
        public static final int NO_NEIGHBOR_DROPS = 32;
        public static final int IS_MOVING = 64;
        public static final int DEFAULT = 3;
        public static final int DEFAULT_AND_RERENDER = 11;*/
    }

    public IItemHandler createHandler() {
        return new ItemStackHandler(1) {
            @Override
            protected void onLoad() {
                super.onLoad();
            }


            @Override
            protected void onContentsChanged(int slot) {
                update();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                //System.out.println("Is Valid");

                ItemStack filterOnPedestal = getFilterInPedestal();
                if(filterOnPedestal.getItem() instanceof IFilterBase)
                {
                    IFilterBase IFB = (IFilterBase)filterOnPedestal.getItem();
                    //System.out.println(IUB.customSlotLimit(getTile(),stack));
                    return IFB.canAcceptItem(getTile(),stack);
                }

                ItemStack coinOnPedestal = getCoinOnPedestal();
                if(coinOnPedestal.getItem() instanceof IUpgradeBase)
                {
                    IUpgradeBase IUB = (IUpgradeBase)getCoinOnPedestal().getItem();
                    //System.out.println(IUB.customSlotLimit(getTile(),stack));
                    return IUB.customIsValid(getTile(),slot,stack);
                }

                return (slot==0)?(true):(false);
            }

            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            protected int getStackLimit(int slot, @Nonnull ItemStack stack) {

                ItemStack filterOnPedestal = getFilterInPedestal();
                if(filterOnPedestal.getItem() instanceof IFilterBase)
                {
                    IFilterBase IFB = (IFilterBase)filterOnPedestal.getItem();
                    //System.out.println(IUB.customSlotLimit(getTile(),stack));
                    return IFB.canAcceptCount(getTile(), getWorld(),getPos(),getItemInPedestal(),stack);
                }

                ItemStack coinOnPedestal = getCoinOnPedestal();
                if(coinOnPedestal.getItem() instanceof IUpgradeBase)
                {
                    IUpgradeBase IUB = (IUpgradeBase)getCoinOnPedestal().getItem();
                    return IUB.canAcceptCount(world,pos,getItemInPedestal(),stack);
                }
                else return super.getStackLimit(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {

                ItemStack coinOnPedestal = getCoinOnPedestal();
                if(coinOnPedestal.getItem() instanceof IUpgradeBase)
                {
                    IUpgradeBase IUB = (IUpgradeBase)getCoinOnPedestal().getItem();
                    //System.out.println(IUB.customSlotLimit(getTile(),stack));
                    if(IUB.customSlotLimit(getTile())!=-1)
                    {
                        return IUB.customSlotLimit(getTile());
                    }
                }
                return super.getSlotLimit(slot);
            }

            @Nonnull
            @Override
            public ItemStack getStackInSlot(int slot) {
                if(slot == -1)
                {
                    return super.getStackInSlot(0);
                }
                else if(getCoinOnPedestal().getItem() instanceof IUpgradeBase)
                {
                    IUpgradeBase IUB = (IUpgradeBase)getCoinOnPedestal().getItem();
                    if(!IUB.customStackInSlot(getTile(),super.getStackInSlot(slot)).getItem().equals(Items.COMMAND_BLOCK))
                    {
                        return IUB.customStackInSlot(getTile(),super.getStackInSlot(slot));
                    }
                }
                return super.getStackInSlot(slot);
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                //System.out.println("AttemptInsert");
                if(slot==-1)
                {
                    return super.insertItem(0, stack, simulate);
                }
                else if(getCoinOnPedestal().getItem() instanceof IUpgradeBase)
                {
                    IUpgradeBase IUB = (IUpgradeBase)getCoinOnPedestal().getItem();
                    //System.out.println("Coin: "+IUB.customInsertItem(getTile(),stack, true).getItem().getName().toString());
                    if(!IUB.customInsertItem(getTile(),stack, true).getItem().equals(Items.COMMAND_BLOCK))
                    {
                        //System.out.println("Ped Insert Custom Return: "+ IUB.customInsertItem(getTile(),stack, true));
                        return IUB.customInsertItem(getTile(),stack, simulate);
                    }
                    else
                    {
                        //System.out.println("Has Coin Ped Insert: "+ super.insertItem(slot, stack, true));
                        return super.insertItem(slot, stack, simulate);
                    }
                }
                //System.out.println("Ped Insert: "+ super.insertItem(slot, stack, true));
                return super.insertItem(slot, stack, simulate);
            }

            @Nonnull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {

                if(slot==-1)
                {
                    //System.out.println("Override: "+super.extractItem(0, amount, true));
                    return super.extractItem(0, amount, simulate);
                }
                else if(getCoinOnPedestal().getItem() instanceof IUpgradeBase && getCoinOnPedestal().getItem().equals(this.stacks.get(slot)))
                {
                    IUpgradeBase IUB = (IUpgradeBase)getCoinOnPedestal().getItem();
                    if(!IUB.customExtractItem(getTile(),amount, true).getItem().equals(Items.COMMAND_BLOCK))
                    {
                        return IUB.customExtractItem(getTile(),amount, simulate);
                    }
                    else
                    {
                        return IUB.canSendItem(getTile())?(super.extractItem(slot, amount, simulate)):(ItemStack.EMPTY);
                    }
                }

                else return super.extractItem(slot, amount, simulate);
            }
        };
    }

    public PedestalTileEntity getTile()
    {
        return this;
    }

    ResourceLocation grabTools = new ResourceLocation("pedestals", "pedestal_tool_whitelist");
    ITag<Item> GET_TOOLS = ItemTags.getCollection().get(grabTools);

    ResourceLocation grabNotTools = new ResourceLocation("pedestals", "pedestal_tool_blacklist");
    ITag<Item> GET_NOTTOOLS = ItemTags.getCollection().get(grabNotTools);

    //Pack Devs *Could* modify this list, but its really only intended for the mod dev as a temp solution.
    ResourceLocation filtersBreakTheseUpgrades = new ResourceLocation("pedestals", "filter_broke_these_upgrades");
    ITag<Item> FILTER_BROKE_UPGRADES = ItemTags.getCollection().get(filtersBreakTheseUpgrades);

    private IItemHandler createHandlerPedestalPrivate() {
        //going from 5 to 11 slots to future proof things
        return new ItemStackHandler(12) {

            @Override
            protected void onLoad() {
                if(getSlots()<12)
                {
                    for(int i = 0; i < getSlots(); ++i) {
                        stacksList.add(i,getStackInSlot(i));
                    }
                    setSize(12);
                    for(int j = 0;j<stacksList.size();j++) {
                        setStackInSlot(j, stacksList.get(j));
                    }
                }

                super.onLoad();
            }

            @Override
            protected void onContentsChanged(int slot) {
                if(!(stacksList.size()>0))
                {
                    update();
                }
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                if (slot == 0 && stack.getItem() instanceof IUpgradeBase && !hasCoin() && ((hasFilter())?(!FILTER_BROKE_UPGRADES.contains(stack.getItem())):(true))) return true;
                if (slot == 1 && stack.getItem().equals(Items.GLOWSTONE) && !hasLight()) return true;
                if (slot == 2 && stack.getItem().equals(ItemPedestalUpgrades.SPEED) && getSpeed()<5) return true;
                if (slot == 3 && stack.getItem().equals(ItemPedestalUpgrades.CAPACITY) && getCapacity()<5) return true;
                if (slot == 4 && stack.getItem().equals(ItemPedestalUpgrades.RANGE) && getRange()<5) return true;
                if (slot == 5 &&
                        (       stack.getItem() instanceof ToolItem
                                || stack.getItem() instanceof SwordItem
                                || stack.getToolTypes().contains(ToolType.get("sword"))
                                || stack.getToolTypes().contains(ToolType.PICKAXE)
                                || stack.getToolTypes().contains(ToolType.HOE)
                                || stack.getToolTypes().contains(ToolType.AXE)
                                || stack.getToolTypes().contains(ToolType.SHOVEL)
                                || GET_TOOLS.contains(stack.getItem())
                        ) && !GET_NOTTOOLS.contains(stack.getItem()) && !hasTool()) return true;
                if (slot == 6 && stack.getItem() instanceof IFilterBase && !stack.getItem().equals(ItemFilterBase.BASEFILTER) && !hasFilter() && ((hasCoin())?(!FILTER_BROKE_UPGRADES.contains(getCoinOnPedestal().getItem())):(true))) return true;
                if (slot == 7 && stack.getItem().equals(Items.REDSTONE_TORCH) && !hasTorch()) return true;
                if (slot == 8 && stack.getItem().equals(ItemPedestalUpgrades.ROUNDROBIN) && !hasRRobin()) return true;
                if (slot == 9 && stack.getItem().equals(ItemPedestalUpgrades.SOUNDMUFFLER)) return true;
                if (slot == 10 && stack.getItem().equals(ItemPedestalUpgrades.PARTICLEDIFFUSER)) return true;
                if (slot == 11 && stack.getItem().equals(ItemPedestalRenderAugment.RENDERAUGMENT)) return true;
                return false;
            }
        };
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return handler.cast();
        }
        if ((cap == CapabilityEnergy.ENERGY) && (getCoinOnPedestal().getItem() instanceof ItemUpgradeBaseEnergy)) {
            return energyHandler.cast();
        }
        if ((cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) && (getCoinOnPedestal().getItem() instanceof ItemUpgradeBaseFluid)) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public int getSlotSizeLimit() {
        IItemHandler h = handler.orElse(null);
        return h.getSlotLimit(0);
    }

    public void spawnItemStack(World worldIn, double x, double y, double z, ItemStack stack) {
        Random RANDOM = new Random();
        double d0 = (double) EntityType.ITEM.getWidth();
        double d1 = 1.0D - d0;
        double d2 = d0 / 2.0D;
        double d3 = Math.floor(x) + RANDOM.nextDouble() * d1 + d2;
        double d4 = Math.floor(y) + RANDOM.nextDouble() * d1;
        double d5 = Math.floor(z) + RANDOM.nextDouble() * d1 + d2;

        while(!stack.isEmpty()) {
            ItemEntity itementity = new ItemEntity(worldIn, d3, d4, d5, stack.split(RANDOM.nextInt(21) + 10));
            float f = 0.05F;
            itementity.setMotion(RANDOM.nextGaussian() * 0.05000000074505806D, RANDOM.nextGaussian() * 0.05000000074505806D + 0.20000000298023224D, RANDOM.nextGaussian() * 0.05000000074505806D);
            worldIn.addEntity(itementity);
        }
    }

    public void dropInventoryItems(World worldIn, BlockPos pos) {
        IItemHandler h = handler.orElse(null);
        for(int i = 0; i < h.getSlots(); ++i) {
            spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), h.getStackInSlot(-1));
        }
    }

    public void dropInventoryItemsPrivate(World worldIn, BlockPos pos) {
        IItemHandler ph = privateHandler.orElse(null);
        for(int i = 0; i < ph.getSlots(); ++i) {
            if(i==0 && hasCoin())
            {
                IUpgradeBase coin = ((IUpgradeBase)ph.getStackInSlot(0).getItem());
                ItemStack actualCoin = ph.getStackInSlot(0);
                coin.removePlayerFromCoin(actualCoin);
                coin.removeWorkQueueFromCoin(actualCoin);
                coin.removeWorkQueueTwoFromCoin(actualCoin);
                coin.removeStoredIntFromCoin(actualCoin);
                coin.removeStoredIntTwoFromCoin(actualCoin);
                coin.removeFilterQueueHandler(actualCoin);
                coin.removeFilterBlock(actualCoin);
                coin.removeInventoryQueue(actualCoin);
                coin.removeCraftingQueue(actualCoin);
            }
            spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), ph.getStackInSlot(i));
        }
    }

    public int maxStackSize(){return 64;}



    /*============================================================================
    ==============================================================================
    =========================    STORED VALUE START    ===========================
    ==============================================================================
    ============================================================================*/

    public int getStoredValueForUpgrades()
    {
        return storedValueForUpgrades;
    }

    public void setStoredValueForUpgrades(int value)
    {
        storedValueForUpgrades = value;
        update();
    }

    /*============================================================================
    ==============================================================================
    =========================     STORED VALUE END     ===========================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================     LINKING START    =============================
    ==============================================================================
    ============================================================================*/

    public int getNumberOfStoredLocations() {return storedLocations.size();}

    public boolean storeNewLocation(BlockPos pos)
    {
        boolean returner = false;
        if(getNumberOfStoredLocations() < 8)
        {
            storedLocations.add(pos);
            returner=true;
        }
        update();
        return returner;
    }

    public BlockPos getStoredPositionAt(int index)
    {
        BlockPos sendToPos = getPos();
        if(index<getNumberOfStoredLocations())
        {
            sendToPos = storedLocations.get(index);
        }

        return sendToPos;
    }

    public boolean removeLocation(BlockPos pos)
    {
        boolean returner = false;
        if(getNumberOfStoredLocations() >= 1)
        {
            storedLocations.remove(pos);
            returner=true;
        }
        update();

        return returner;
    }

    public boolean isAlreadyLinked(BlockPos pos) {
        return storedLocations.contains(pos);
    }

    public List<BlockPos> getLocationList()
    {
        return storedLocations;
    }

    public int getLinkingRange()
    {
        int range = 8;
        switch (getRange())
        {
            case 0:
                range = (getRange()>0)?(8):(8);//normal speed
                break;
            case 1:
                range=(getRange()>0)?(12):(8);//2x faster
                break;
            case 2:
                range = (getRange()>0)?(16):(8);;//4x faster
                break;
            case 3:
                range = (getRange()>0)?(32):(8);;//6x faster
                break;
            case 4:
                range = (getRange()>0)?(48):(8);;//10x faster
                break;
            case 5:
                range=(getRange()>0)?(64):(8);;//20x faster
                break;
            default: range=8;
        }
        return  range;
    }

    public boolean isPedestalInRange(PedestalTileEntity pedestalCurrent, BlockPos pedestalToBeLinked)
    {
        int range = pedestalCurrent.getLinkingRange();
        int x = pedestalToBeLinked.getX();
        int y = pedestalToBeLinked.getY();
        int z = pedestalToBeLinked.getZ();
        int x1 = pedestalCurrent.getPos().getX();
        int y1 = pedestalCurrent.getPos().getY();
        int z1 = pedestalCurrent.getPos().getZ();
        int xF = Math.abs(Math.subtractExact(x,x1));
        int yF = Math.abs(Math.subtractExact(y,y1));
        int zF = Math.abs(Math.subtractExact(z,z1));

        if(xF>range || yF>range || zF>range)
        {
            return false;
        }
        else return true;
    }

    /*============================================================================
    ==============================================================================
    ===========================      LINKING END     =============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================     ITEM START       =============================
    ==============================================================================
    ============================================================================*/

    public boolean hasItem()
    {
        IItemHandler h = handler.orElse(null);
        if(h.getStackInSlot(0).isEmpty())
        {
            return false;
        }
        else  return true;
    }

    public ItemStack getItemInPedestal()
    {
        IItemHandler h = handler.orElse(null);
        if(hasItem())
        {
            return h.getStackInSlot(0);
        }
        else return ItemStack.EMPTY;
    }

    public ItemStack getItemInPedestalOverride()
    {
        IItemHandler h = handler.orElse(null);
        return h.getStackInSlot(-1);
    }

    public ItemStack removeItem(int numToRemove) {
        IItemHandler h = handler.orElse(null);
        ItemStack stack = h.extractItem(0,numToRemove,false);
        //update();

        return stack;
    }

    public ItemStack removeItemOverride(int numToRemove) {
        IItemHandler h = handler.orElse(null);
        ItemStack stack = h.extractItem(-1,numToRemove,false);
        //update();

        return stack;
    }

    public ItemStack removeItem() {
        IItemHandler h = handler.orElse(null);
        ItemStack stack = h.extractItem(0,getItemInPedestal().getMaxStackSize(),false);
        //update();

        return stack;
    }

    public ItemStack removeItemOverride() {
        IItemHandler h = handler.orElse(null);
        ItemStack stack = h.extractItem(-1,h.getStackInSlot(0).getCount(),false);
        //update();

        return stack;
    }

    public boolean addItem(ItemStack itemFromBlock)
    {
        IItemHandler h = handler.orElse(null);
        if(hasItem())
        {
            if(ItemHandlerHelper.canItemStacksStack(getItemInPedestal(),itemFromBlock))
            {
                h.insertItem(0, itemFromBlock.copy(), false);
            }
        }
        else {h.insertItem(0, itemFromBlock.copy(), false);}
        //update();

        return true;
    }

    public boolean addItemOverride(ItemStack itemFromBlock)
    {
        IItemHandler h = handler.orElse(null);
        ItemStack overrideInPedestal = getItemInPedestalOverride();
        if(!overrideInPedestal.isEmpty())
        {
            if(ItemStack.areItemsEqual(overrideInPedestal,itemFromBlock))
            {
                return h.insertItem(-1, itemFromBlock.copy(), false).isEmpty();
            }
        }
        else {return h.insertItem(-1, itemFromBlock.copy(), false).isEmpty();}
        update();

        return false;
    }

    public ItemStack addItemStackOverride(ItemStack itemFromBlock)
    {
        IItemHandler h = handler.orElse(null);
        return h.insertItem(-1, itemFromBlock.copy(), false);
    }

    public boolean addItem(ItemStack itemFromBlock,boolean simulate)
    {
        IItemHandler h = handler.orElse(null);
        if(hasItem())
        {
            if(ItemHandlerHelper.canItemStacksStack(getItemInPedestal(),itemFromBlock))
            {
                if(!simulate)
                {
                    h.insertItem(0, itemFromBlock.copy(), false);
                    //update();
                }
                return true;
            }
        }
        else
        {
            if(!simulate)
            {
                h.insertItem(0, itemFromBlock.copy(), false);
                //update();
            }
            return true;
        }

        return false;
    }

    public ItemStack addItemCustom(ItemStack itemstackIn,boolean simulate)
    {
        IItemHandler h = handler.orElse(null);
        return h.insertItem(0, itemstackIn.copy(), simulate);
    }

    public int getItemTransferRate()
    {
        int itemRate = 4;
        switch (getCapacity())
        {
            case 0:
                itemRate = (getCapacity()>0)?(4):(4);
                break;
            case 1:
                itemRate= (getCapacity()>0)?(8):(4);
                break;
            case 2:
                itemRate = (getCapacity()>0)?(16):(4);
                break;
            case 3:
                itemRate = (getCapacity()>0)?(32):(4);
                break;
            case 4:
                itemRate = (getCapacity()>0)?(48):(4);
                break;
            case 5:
                itemRate=(getCapacity()>0)?(64):(4);
                break;
            default: itemRate=4;
        }

        return  itemRate;
    }

    public void collideWithPedestal(World world, PedestalTileEntity tilePedestal, BlockPos posPedestal, BlockState state, Entity entityIn)
    {
        if(!world.isRemote) {
            if(entityIn instanceof ItemEntity)
            {
                if(tilePedestal.hasCoin())
                {
                    Item coinInPed = tilePedestal.getCoinOnPedestal().getItem();
                    if(coinInPed instanceof IUpgradeBase)
                    {
                        ((IUpgradeBase) coinInPed).actionOnCollideWithBlock(tilePedestal, ((ItemEntity)entityIn));
                    }
                }
            }
        }
    }

    /*============================================================================
    ==============================================================================
    ===========================      ITEM END        =============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================      COIN START      =============================
    ==============================================================================
    ============================================================================*/

    public boolean hasCoin()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(0).isEmpty())
        {
            return false;
        }
        else  return true;
    }

    public ItemStack getCoinOnPedestal()
    {
        IItemHandler ph = privateHandler.orElse(null);
        return ph.getStackInSlot(0);
        /*if(hasCoin())
        {
            return ph.getStackInSlot(0);
        }
        else return ItemStack.EMPTY;*/
    }

    public ItemStack removeCoin() {
        IItemHandler ph = privateHandler.orElse(null);
        ItemStack stack = ph.getStackInSlot(0);
        if(stack.getItem() instanceof IUpgradeBase){
            IUpgradeBase coin = (IUpgradeBase)stack.getItem();
            coin.removePlayerFromCoin(stack);
            coin.removeWorkQueueFromCoin(stack);
            coin.removeWorkQueueTwoFromCoin(stack);
            coin.removeStoredIntFromCoin(stack);
            coin.removeStoredIntTwoFromCoin(stack);
            coin.removeFilterQueueHandler(stack);
            coin.removeFilterBlock(stack);
            coin.removeInventoryQueue(stack);
            coin.removeCraftingQueue(stack);
            coin.removeOutputIngredientMap(stack);
            coin.removeFilterChangeUpdated(stack);
            coin.removeToolChangeUpdated(stack);
        }
        ph.extractItem(0,stack.getCount(),false);
        //update();

        return stack;
    }

    public boolean addCoin(PlayerEntity player, ItemStack coinFromBlock,boolean simulate)
    {
        if(hasCoin())
        {
            return false;
        }
        else
        {
            IItemHandler ph = privateHandler.orElse(null);
            ItemStack coinItem = coinFromBlock.copy();
            coinItem.setCount(1);
            if(!hasCoin() && ph.isItemValid(0,coinItem))
            {
                if(!simulate)
                {
                    ((IUpgradeBase)coinFromBlock.getItem()).setPlayerOnCoin(coinFromBlock,player);
                    ph.insertItem(0,coinItem,false);


                }
                return true;
            }
            else return false;
        }
    }

    /*============================================================================
    ==============================================================================
    ===========================       COIN END       =============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================     SPEED START      =============================
    ==============================================================================
    ============================================================================*/

    public boolean addSpeed(ItemStack speedUpgrade)
    {
        IItemHandler ph = privateHandler.orElse(null);
        ItemStack itemFromBlock = speedUpgrade.copy();
        itemFromBlock.setCount(1);
        if(getSpeed() < 5)
        {
            ph.insertItem(2,itemFromBlock,false);
            //update();
            return true;
        }
        else return false;
    }

    public ItemStack removeSpeed(int count)
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(hasSpeed())
        {
            //update();
            return ph.extractItem(2,count,false);
        }
        else return ItemStack.EMPTY;
    }

    public ItemStack removeSpeed()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(hasSpeed())
        {
            //update();
            return ph.extractItem(2,ph.getStackInSlot(2).getCount(),false);
        }
        else return ItemStack.EMPTY;
    }

    public boolean hasSpeed()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(2).isEmpty())
        {
            return false;
        }
        else  return true;
    }

    public int getSpeed()
    {
        IItemHandler ph = privateHandler.orElse(null);
        return ph.getStackInSlot(2).getCount();
    }

    public int getOperationSpeed()
    {
        int speed = 20;
        switch (getSpeed())
        {
            case 0:
                speed = (getSpeed()>0)?(20):(20);//normal speed
                break;
            case 1:
                speed=(getSpeed()>0)?(10):(20);//2x faster
                break;
            case 2:
                speed = (getSpeed()>0)?(5):(20);;//4x faster
                break;
            case 3:
                speed = (getSpeed()>0)?(3):(20);;//6x faster
                break;
            case 4:
                speed = (getSpeed()>0)?(2):(20);;//10x faster
                break;
            case 5:
                speed=(getSpeed()>0)?(1):(20);;//20x faster
                break;
            default: speed=20;
        }

        return  speed;
    }

    /*============================================================================
    ==============================================================================
    ===========================      SPEED END       =============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================    CAPACITY START    =============================
    ==============================================================================
    ============================================================================*/

    public boolean addCapacity(ItemStack capacityUpgrade)
    {
        IItemHandler ph = privateHandler.orElse(null);
        ItemStack itemFromBlock = capacityUpgrade.copy();
        itemFromBlock.setCount(1);
        if(getCapacity() < 5)
        {
            ph.insertItem(3,itemFromBlock,false);
            //update();
            return true;
        }
        else return false;
    }

    public ItemStack removeCapacity(int count)
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(hasCapacity())
        {
            //update();
            return ph.extractItem(3,count,false);
        }
        else return ItemStack.EMPTY;
    }

    public ItemStack removeCapacity()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(hasCapacity())
        {
            //update();
            return ph.extractItem(3,ph.getStackInSlot(3).getCount(),false);
        }
        else return ItemStack.EMPTY;
    }

    public boolean hasCapacity()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(3).isEmpty())
        {
            return false;
        }
        else  return true;
    }
    public int getCapacity()
    {
        IItemHandler ph = privateHandler.orElse(null);
        return ph.getStackInSlot(3).getCount();
    }

    /*============================================================================
    ==============================================================================
    ===========================     CAPACITY END     =============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================     RANGE START      =============================
    ==============================================================================
    ============================================================================*/

    public boolean addRange(ItemStack rangeUpgrade)
    {
        IItemHandler ph = privateHandler.orElse(null);
        ItemStack itemFromBlock = rangeUpgrade.copy();
        itemFromBlock.setCount(1);
        if(getRange() < 5)
        {
            ph.insertItem(4,itemFromBlock,false);
            //update();
            return true;
        }
        else return false;
    }

    public ItemStack removeRange(int count)
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(hasRange())
        {
            //update();
            return ph.extractItem(4,count,false);
        }
        else return ItemStack.EMPTY;
    }

    public ItemStack removeRange()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(hasRange())
        {
            //update();
            return ph.extractItem(4,ph.getStackInSlot(4).getCount(),false);
        }
        else return ItemStack.EMPTY;
    }

    public int getPedestalTransferRange(){return getRange();}

    public boolean hasRange()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(4).isEmpty())
        {
            return false;
        }
        else  return true;
    }
    public int getRange()
    {
        IItemHandler ph = privateHandler.orElse(null);
        return ph.getStackInSlot(4).getCount();
    }

    /*============================================================================
    ==============================================================================
    ===========================      RANGE END       =============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================      LIGHT START     =============================
    ==============================================================================
    ============================================================================*/

    public boolean addLight()
    {
        if(hasLight())
        {
            return false;
        }
        else
        {
            boolLight = true;
            BlockState state = world.getBlockState(pos);
            boolean watered = state.get(PedestalBlock.WATERLOGGED);
            Direction dir = state.get(PedestalBlock.FACING);
            int filterState = state.get(PedestalBlock.FILTER_STATUS);
            BlockState newstate = state.with(PedestalBlock.FACING,dir).with(PedestalBlock.WATERLOGGED,watered).with(PedestalBlock.LIT,true).with(PedestalBlock.FILTER_STATUS,filterState);
            IItemHandler ph = privateHandler.orElse(null);
            ph.insertItem(1,new ItemStack(Items.GLOWSTONE,1),false);
            world.notifyBlockUpdate(pos,state,newstate,3);
            world.setBlockState(pos,newstate,3);
            world.markBlockRangeForRenderUpdate(pos,state,newstate);
            //update();
            return true;
        }
    }

    public boolean hasLight()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(1).isEmpty())
        {
            return false;
        }
        else  return true;
    }

    /*============================================================================
    ==============================================================================
    ===========================       LIGHT END      =============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================     COLOR START      =============================
    ==============================================================================
    ============================================================================*/

    public boolean addColor(ItemStack stack)
    {
        if(!hasCoin() && !hasItem() && !hasLight() && !hasSpeed() && !hasCapacity() && !hasRange() && getNumberOfStoredLocations() <= 0)
        {
            int intColor = stack.getTag().getInt("color");
            BlockState replacestate = CraftingPedestals.instance().getResult(intColor);
            BlockState state = world.getBlockState(pos);
            if(replacestate.getBlock().equals(state.getBlock()))
            {
                return false;
            }
            else
            {
                boolean watered = state.get(PedestalBlock.WATERLOGGED);
                Direction dir = state.get(PedestalBlock.FACING);
                boolean lit = state.get(PedestalBlock.LIT);
                int filterState = state.get(PedestalBlock.FILTER_STATUS);
                BlockState newstate = replacestate.with(PedestalBlock.FACING,dir).with(PedestalBlock.WATERLOGGED,watered).with(PedestalBlock.LIT,lit).with(PedestalBlock.FILTER_STATUS,filterState);
                world.notifyBlockUpdate(pos,state,newstate,3);
                world.setBlockState(pos,newstate,3);
                //update();
                return true;
            }
        }
        else
        {
            return false;
        }
    }

    /*============================================================================
    ==============================================================================
    ===========================       COLOR END      =============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================      TOOL START      =============================
    ==============================================================================
    ============================================================================*/

    public boolean addTool(ItemStack tool,boolean simulate)
    {
        IItemHandler ph = privateHandler.orElse(null);
        //CompoundNBT nbt = this.serializeNBT();
        //int slotSize = (nbt.getCompound("invp").contains("Size"))?(nbt.getCompound("invp").getInt("Size")):(5);
        //if(slotSize > 5)
        //{
        ItemStack itemFromBlock = tool.copy();
        itemFromBlock.setCount(1);
        if(!hasTool() && ph.isItemValid(5,itemFromBlock))
        {
            if(!simulate)ph.insertItem(5,itemFromBlock,false);
            return true;
        }
        else return false;
        //}

        //return false;
    }

    public boolean hasTool()
    {
        IItemHandler ph = privateHandler.orElse(null);
        /*CompoundNBT nbt = this.serializeNBT();
        int slotSize = (nbt.getCompound("invp").contains("Size"))?(nbt.getCompound("invp").getInt("Size")):(5);
        if(slotSize > 5)*/
        //{
        if(ph.getStackInSlot(5).isEmpty())
        {
            return false;
        }
        else  return true;
        //}

        //return false;
    }

    public ItemStack getToolOnPedestal()
    {
        IItemHandler ph = privateHandler.orElse(null);
        return ph.getStackInSlot(5);
    }

    public ItemStack removeTool() {
        IItemHandler ph = privateHandler.orElse(null);
        return ph.extractItem(5,ph.getStackInSlot(5).getCount(),false);
    }

    /*============================================================================
    ==============================================================================
    ===========================       TOOL END       =============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================     FILTER START     =============================
    ==============================================================================
    ============================================================================*/

    public boolean hasFilter()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(6).isEmpty())
        {
            return false;
        }
        else  return true;
    }

    public ItemStack getFilterInPedestal()
    {
        IItemHandler ph = privateHandler.orElse(null);
        return ph.getStackInSlot(6);
    }

    public ItemStack removeFilter(boolean updateBlock) {
        IItemHandler ph = privateHandler.orElse(null);
        if(updateBlock)
        {
            BlockState state = world.getBlockState(pos);
            int filterState = 0;
            boolean watered = state.get(PedestalBlock.WATERLOGGED);
            Direction dir = state.get(PedestalBlock.FACING);
            boolean lit = state.get(PedestalBlock.LIT);
            BlockState newstate = state.with(PedestalBlock.FACING,dir).with(PedestalBlock.WATERLOGGED,watered).with(PedestalBlock.LIT,lit).with(PedestalBlock.FILTER_STATUS,filterState);
            world.notifyBlockUpdate(pos,state,newstate,3);
            world.setBlockState(pos,newstate,3);
            world.markBlockRangeForRenderUpdate(pos,state,newstate);
        }

        if(hasCoin())
        {
            if(getCoinOnPedestal().getItem() instanceof IUpgradeBase)
            {
                IUpgradeBase coin = ((IUpgradeBase)getCoinOnPedestal().getItem());
                coin.setFilterChangeUpdate(getCoinOnPedestal());
            }
        }
        return ph.extractItem(6,ph.getStackInSlot(6).getCount(),false);
    }

    public boolean addFilter(ItemStack filter,boolean simulate)
    {
        if(hasFilter())
        {
            return false;
        }
        else
        {
            BlockState state = world.getBlockState(pos);
            boolean watered = state.get(PedestalBlock.WATERLOGGED);
            Direction dir = state.get(PedestalBlock.FACING);
            boolean lit = state.get(PedestalBlock.LIT);
            int filterState = state.get(PedestalBlock.FILTER_STATUS);
            if(filter.getItem() instanceof IFilterBase)
            {
                //Blacklist = 2 , whitelist = 1
                filterState = (((IFilterBase) filter.getItem()).getFilterTypeFromNBT(filter))?(2):(1);
            }
            BlockState newstate = state.with(PedestalBlock.FACING,dir).with(PedestalBlock.WATERLOGGED,watered).with(PedestalBlock.LIT,lit).with(PedestalBlock.FILTER_STATUS,filterState);

            IItemHandler ph = privateHandler.orElse(null);
            ItemStack filterItem = filter.copy();
            filterItem.setCount(1);
            if(!hasFilter() && ph.isItemValid(6,filterItem))
            {
                if(!simulate)
                {
                    ph.insertItem(6,filterItem,false);
                    world.notifyBlockUpdate(pos,state,newstate,3);
                    world.setBlockState(pos,newstate,3);
                    world.markBlockRangeForRenderUpdate(pos,state,newstate);

                    if(hasCoin())
                    {
                        if(getCoinOnPedestal().getItem() instanceof IUpgradeBase)
                        {
                            IUpgradeBase coin = ((IUpgradeBase)getCoinOnPedestal().getItem());
                            coin.setFilterChangeUpdate(getCoinOnPedestal());
                        }
                    }
                }
                return true;
            }
            else return false;
        }
    }

    public boolean updateFilter(ItemStack filter,boolean updateBlock)
    {
        if(hasFilter())
        {
            //old filter needing updated
            ItemStack oldStack = getFilterInPedestal().copy();

            BlockState state = world.getBlockState(pos);
            int filterState = state.get(PedestalBlock.FILTER_STATUS);
            int newFilterState = filterState;
            if(filter.getItem() instanceof IFilterBase)
            {
                //Blacklist = 2 , whitelist = 1
                newFilterState = (((IFilterBase) filter.getItem()).getFilterTypeFromNBT(filter))?(2):(1);

                if(filter.hasTag())
                {
                    oldStack.setTag(filter.getTag());

                    if(hasCoin())
                    {
                        if(getCoinOnPedestal().getItem() instanceof IUpgradeBase)
                        {
                            IUpgradeBase coin = ((IUpgradeBase)getCoinOnPedestal().getItem());
                            coin.setFilterChangeUpdate(getCoinOnPedestal());
                        }
                    }
                }
            }
            IItemHandler ph = privateHandler.orElse(null);
            if(ph.isItemValid(6,filter))
            {
                if(updateBlock) {
                    removeFilter(false);
                    ph.insertItem(6, oldStack, false);
                    if(newFilterState != filterState)
                    {
                        boolean watered = state.get(PedestalBlock.WATERLOGGED);
                        Direction dir = state.get(PedestalBlock.FACING);
                        boolean lit = state.get(PedestalBlock.LIT);
                        BlockState newstate = state.with(PedestalBlock.FACING,dir).with(PedestalBlock.WATERLOGGED,watered).with(PedestalBlock.LIT,lit).with(PedestalBlock.FILTER_STATUS,newFilterState);
                        world.notifyBlockUpdate(pos,state,newstate,3);
                        world.setBlockState(pos,newstate,3);
                        world.markBlockRangeForRenderUpdate(pos,state,newstate);

                        if(hasCoin())
                        {
                            if(getCoinOnPedestal().getItem() instanceof IUpgradeBase)
                            {
                                IUpgradeBase coin = ((IUpgradeBase)getCoinOnPedestal().getItem());
                                coin.setFilterChangeUpdate(getCoinOnPedestal());
                            }
                        }
                    }
                }
                return true;
            }
        }

        return false;
    }

    /*============================================================================
    ==============================================================================
    ===========================      FILTER END      =============================
    ==============================================================================
    ============================================================================*/

    /*============================================================================
    ==============================================================================
    ===========================    REDSTONE START    =============================
    ==============================================================================
    ============================================================================*/

    public boolean hasTorch()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(7).isEmpty())
        {
            return false;
        }
        else  return true;
    }

    public ItemStack removeTorch() {
        IItemHandler ph = privateHandler.orElse(null);
        return ph.extractItem(7,ph.getStackInSlot(7).getCount(),false);
    }

    public boolean addTorch()
    {
        IItemHandler ph = privateHandler.orElse(null);

        ItemStack itemFromBlock = new ItemStack(Items.REDSTONE_TORCH);
        itemFromBlock.setCount(1);
        if(!hasTorch() && ph.isItemValid(7,itemFromBlock))
        {
            ph.insertItem(7,itemFromBlock,false);
            return true;
        }
        else return false;
    }

    public boolean isPedestalBlockPowered(World world,BlockPos pos)
    {
        boolean returner = world.isBlockPowered(pos);
        if(hasTorch())
        {
            return !returner;
        }

        return returner;
    }

    /*============================================================================
    ==============================================================================
    ===========================    REDSTONE END      =============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================     Robin START     ==============================
    ==============================================================================
    ============================================================================*/

    public boolean addRRobin(ItemStack roundRobin)
    {
        IItemHandler ph = privateHandler.orElse(null);
        ItemStack itemFromBlock = roundRobin.copy();
        itemFromBlock.setCount(1);
        if(!hasRRobin())
        {
            ph.insertItem(8,itemFromBlock,false);
            //update();
            return true;
        }
        else return false;
    }

    public ItemStack removeRRobin()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(hasRRobin())
        {
            //update();
            return ph.extractItem(8,ph.getStackInSlot(8).getCount(),false);
        }
        else return ItemStack.EMPTY;
    }

    public boolean hasRRobin()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(8).isEmpty())
        {
            return false;
        }
        else  return true;
    }

    /*============================================================================
    ==============================================================================
    ===========================      Robin END      ==============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================    MUFFLER START    ==============================
    ==============================================================================
    ============================================================================*/

    public boolean addMuffler(ItemStack muffler)
    {
        IItemHandler ph = privateHandler.orElse(null);
        ItemStack itemFromBlock = muffler.copy();
        itemFromBlock.setCount(1);
        if(!hasMuffler())
        {
            ph.insertItem(9,itemFromBlock,false);
            //update();
            return true;
        }
        else return false;
    }

    public ItemStack removeMuffler()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(hasMuffler())
        {
            //update();
            return ph.extractItem(9,ph.getStackInSlot(9).getCount(),false);
        }
        else return ItemStack.EMPTY;
    }

    public boolean hasMuffler()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(9).isEmpty())
        {
            return false;
        }
        else  return true;
    }

    /*============================================================================
    ==============================================================================
    ===========================     MUFFLER END     ==============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ===========================   PARTICLE START    ==============================
    ==============================================================================
    ============================================================================*/

    public ItemStack addParticleDiffuser(ItemStack particle)
    {
        IItemHandler ph = privateHandler.orElse(null);
        ItemStack itemFromBlock = particle.copy();
        itemFromBlock.setCount(1);
        if(!hasParticleDiffuser())
        {
            //update();
            return ph.insertItem(10,itemFromBlock,false);
        }
        else return ItemStack.EMPTY;
    }

    public ItemStack removeParticleDiffuser()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(hasParticleDiffuser())
        {
            //update();
            return ph.extractItem(10,ph.getStackInSlot(10).getCount(),false);
        }
        else return ItemStack.EMPTY;
    }

    public boolean hasParticleDiffuser()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(10).isEmpty())
        {
            return false;
        }
        else  return true;
    }

    /*============================================================================
    ==============================================================================
    ===========================    PARTICLE END     ==============================
    ==============================================================================
    ============================================================================*/



    /*============================================================================
    ==============================================================================
    ============================   RENDER START    ===============================
    ==============================================================================
    ============================================================================*/

    public ItemStack addRenderAugment(ItemStack particle)
    {
        IItemHandler ph = privateHandler.orElse(null);
        ItemStack itemFromBlock = particle.copy();
        itemFromBlock.setCount(1);
        if(!hasRenderAugment())
        {
            //update();
            return ph.insertItem(11,itemFromBlock,false);
        }
        else return ItemStack.EMPTY;
    }

    public ItemStack removeRenderAugment()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(hasRenderAugment())
        {
            //update();
            return ph.extractItem(11,ph.getStackInSlot(11).getCount(),false);
        }
        else return ItemStack.EMPTY;
    }

    public boolean hasRenderAugment()
    {
        IItemHandler ph = privateHandler.orElse(null);
        if(ph.getStackInSlot(11).isEmpty())
        {
            return false;
        }
        else  return true;
    }

    public int getRenderAugmentType()
    {
        // 0 = BOTH
        // 1 = UPGRADE
        // 2 = ITEM
        // 3 = NORMAL RENDER
        IItemHandler ph = privateHandler.orElse(null);
        if(hasRenderAugment())
        {
            if(ph.getStackInSlot(11).getItem() instanceof ItemPedestalRenderAugment)
            {
                ItemPedestalRenderAugment augment = ((ItemPedestalRenderAugment)ph.getStackInSlot(11).getItem());
                return augment.getAugmentType(ph.getStackInSlot(11));
            }
            else  return 0;
        }
        else  return 3;
    }

    /*============================================================================
    ==============================================================================
    ============================    RENDER END     ===============================
    ==============================================================================
    ============================================================================*/














    public boolean canSendItemInPedestal(PedestalTileEntity pedestal)
    {
        if(pedestal.hasItem())
        {
            if(hasCoin())
            {
                Item coin = getCoinOnPedestal().getItem();
                if(coin instanceof IUpgradeBase)
                {
                    return ((IUpgradeBase)coin).canSendItem(pedestal);
                }
            }
            else return true;
        }

        return false;
    }

    public boolean isSamePedestal(BlockPos pedestalToBeLinked)
    {
        BlockPos thisPedestal = this.getPos();

        if(thisPedestal.equals(pedestalToBeLinked))
        {
            return true;
        }

        return false;
    }

    //Checks when linking pedestals if the two being linked are the same block and within range
    public boolean canLinkToPedestalNetwork(BlockPos pedestalToBeLinked)
    {
        //Check to see if pedestal to be linked is a block pedestal
        if(world.getBlockState(pedestalToBeLinked).getBlock() instanceof PedestalBlock)
        {
            //isPedestalInRange(tileCurrent,pedestalToBeLinked);
            return true;
        }

        return false;
    }

    //Returns items available to be insert, 0 if false
    public int canAcceptItems(World worldIn, BlockPos posPedestal, ItemStack itemsIncoming)
    {
        int canAccept = 0;
        int pedestalAccept = 0;
        boolean isTank = false;

        if(hasCoin())
        {
            Item coinInPed = this.getCoinOnPedestal().getItem();
            if(coinInPed instanceof IUpgradeBase)
            {
                pedestalAccept = ((IUpgradeBase) coinInPed).canAcceptCount(worldIn, posPedestal, getItemInPedestal(), itemsIncoming);
            }

            if(coinInPed instanceof ItemUpgradeItemTank)
            {
                if(((ItemUpgradeItemTank) coinInPed).availableStorageSpace(this) > 0)isTank = true;
            }
        }

        if(getItemInPedestal().isEmpty() || getItemInPedestal().equals(ItemStack.EMPTY))
        {
            canAccept = itemsIncoming.getMaxStackSize();
        }
        else
        {
            if(ItemHandlerHelper.canItemStacksStack(getItemInPedestal(),itemsIncoming))
            {
                //Two buckets match but cant be stacked since max stack size is 1
                //BUT if its a tank, its cooler then this
                if(itemsIncoming.getMaxStackSize() > 1 || isTank)
                {
                    //If i did this right, slot limit should default to stack max size, or custom allowed
                    int allowed = getSlotSizeLimit();
                    if(allowed> itemsIncoming.getMaxStackSize() && !isTank)allowed=itemsIncoming.getMaxStackSize();
                    if(getItemInPedestal().getCount() < allowed)
                    {
                        canAccept = (allowed - getItemInPedestal().getCount());
                    }
                }
            }
        }

        if(hasFilter())
        {
            Item filterInPed = this.getFilterInPedestal().getItem();
            if(filterInPed instanceof IFilterBase)
            {
                pedestalAccept = ((IFilterBase) filterInPed).canAcceptCount(getTile(), itemsIncoming);
            }
        }

        if((canAccept > pedestalAccept) && (hasFilter() || hasCoin()))
        {
            canAccept = pedestalAccept;
        }

        return canAccept;
    }

    public boolean hasFilter(PedestalTileEntity pedestalSendingTo)
    {
        boolean returner = false;
        if(pedestalSendingTo.hasFilter())
        {
            Item filterInPedestal = pedestalSendingTo.getFilterInPedestal().getItem();
            if(filterInPedestal instanceof IFilterBase)
            {
                returner = true;
            }
        }

        return returner;
    }

    //Checks to see if we can send an item from the pedestal, like in the case of the import energy/fluid upgrades
    public Boolean canSendItemInPedestal()
    {
        boolean returner = true;

        if(hasCoin())
        {
            Item coinInPedestal = getCoinOnPedestal().getItem();
            if(coinInPedestal instanceof IUpgradeBase)
            {
                return ((IUpgradeBase) coinInPedestal).canSendItem(this);
            }
        }

        return returner;
    }

    public static LazyOptional<IItemHandler> findItemHandlerPedestal(PedestalTileEntity pedestal)
    {
        World world = pedestal.getWorld();
        BlockPos pos = pedestal.getPos();
        TileEntity neighbourTile = world.getTileEntity(pos);
        if(neighbourTile!=null)
        {
            LazyOptional<IItemHandler> cap = neighbourTile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.UP);
            if(cap.isPresent())
                return cap;
        }
        return LazyOptional.empty();
    }

    //Needed for filtered imports
    public boolean canSendToPedestal(BlockPos pedestalToSendTo, ItemStack itemStackIncoming)
    {
        boolean returner = false;

        //Method to check if we can send items FROM this pedestal???
        if(canSendItemInPedestal())
        {
            //Check if Block is Loaded in World
            if(world.isAreaLoaded(pedestalToSendTo,1))
            {
                //If block ISNT powered
                if(!world.isBlockPowered(pedestalToSendTo))
                {
                    //Make sure its a pedestal before getting the tile
                    if(world.getBlockState(pedestalToSendTo).getBlock() instanceof PedestalBlock)
                    {
                        //Make sure it is still part of the right network
                        if(canLinkToPedestalNetwork(pedestalToSendTo))
                        {
                            //Get the tile before checking other things
                            if(world.getTileEntity(pedestalToSendTo) instanceof PedestalTileEntity)
                            {
                                PedestalTileEntity tilePedestalToSendTo = (PedestalTileEntity)world.getTileEntity(pedestalToSendTo);

                                //Checks if pedestal is empty or if not then checks if items match and how many can be insert
                                if(tilePedestalToSendTo.canAcceptItems(world,pedestalToSendTo,itemStackIncoming) > 0)
                                {
                                    boolean filter = true;
                                    boolean coin = true;
                                    //Check if it has filter, if not return true
                                    if(tilePedestalToSendTo.hasFilter())
                                    {
                                        Item filterInPedestal = tilePedestalToSendTo.getFilterInPedestal().getItem();
                                        if(filterInPedestal instanceof IFilterBase)
                                        {
                                            filter = ((IFilterBase) filterInPedestal).canAcceptItem(tilePedestalToSendTo,itemStackIncoming);
                                        }
                                    }

                                    if(tilePedestalToSendTo.hasCoin())
                                    {
                                        Item coinInPedestal = tilePedestalToSendTo.getCoinOnPedestal().getItem();
                                        if(coinInPedestal instanceof IUpgradeBase)
                                        {
                                            coin = ((IUpgradeBase) coinInPedestal).canAcceptItem(getWorld(),pedestalToSendTo,itemStackIncoming);
                                        }
                                    }

                                    //Should return true by default, or fals eif a filter or coin blocks it???
                                    returner = filter && coin;
                                }
                            }
                        }
                    }
                    else
                    {
                        removeLocation(pedestalToSendTo);
                    }
                }
            }
        }

        return returner;
    }




    //The actual transfer methods for items
    public void sendItemsToPedestal(BlockPos pedestalToSendTo)
    {
        if(world.getTileEntity(pedestalToSendTo) instanceof PedestalTileEntity)
        {
            PedestalTileEntity tileToSendTo = ((PedestalTileEntity)world.getTileEntity(pedestalToSendTo));

            //Max that can be recieved
            int countToSend = tileToSendTo.canAcceptItems(world,pedestalToSendTo,getItemInPedestal());
            ItemStack copyStackToSend = getItemInPedestal().copy();
            //Max that is available to send
            if(copyStackToSend.getCount()<countToSend)
            {
                countToSend = copyStackToSend.getCount();
            }
            //Get max that can be sent
            if(countToSend > getItemTransferRate())
            {
                countToSend = getItemTransferRate();
            }


            if(countToSend >=1)
            {
                //Send items
                copyStackToSend.setCount(countToSend);
                removeItem(copyStackToSend.getCount());
                tileToSendTo.addItem(copyStackToSend);
                if(!hasParticleDiffuser())PacketHandler.sendToNearby(world,pos,new PacketParticles(PacketParticles.EffectType.ANY_COLOR_BEAM,pedestalToSendTo.getX(),pedestalToSendTo.getY(),pedestalToSendTo.getZ(),pos.getX(),pos.getY(),pos.getZ()));
            }
        }
    }

    public void transferActionItems()
    {
        int locations = getNumberOfStoredLocations();
        if(locations > 0)
        {
            if(hasRRobin())
            {
                int robinCount = getStoredValueForUpgrades();
                if(robinCount >= locations)
                {
                    setStoredValueForUpgrades(0);
                    robinCount=0;
                }
                BlockPos posReceiver = getStoredPositionAt(robinCount);
                if(canSendToPedestal(posReceiver,getItemInPedestal()))
                {
                    sendItemsToPedestal(posReceiver);
                }

                robinCount++;
                setStoredValueForUpgrades(robinCount);
            }
            else
            {
                for(int i=0;i<locations;i++){
                    BlockPos posReceiver = getStoredPositionAt(i);
                    if(canSendToPedestal(posReceiver,getItemInPedestal()))
                    {
                        sendItemsToPedestal(posReceiver);
                        break;
                    }
                    else continue;
                }
            }
        }
    }


    int partTicker = 0;
    int impTicker = 0;
    int pedTicker = 0;
    @Override
    public void tick() {

        if(!world.isRemote && world.isAreaLoaded(pos,1))
        {
            if(getNumberOfStoredLocations() >0 && !isPedestalBlockPowered(getWorld(),getPos()) && hasItem())
            {
                pedTicker++;
                if (pedTicker%getOperationSpeed() == 0) {

                    transferActionItems();
                    //Eventually have Energy, Fluids, XP in here too???

                    if(pedTicker >=20){pedTicker=0;}
                }
            }
        }
        if(world.isAreaLoaded(pos,1))
        {
            if(hasCoin())
            {
                Item coinInPed = getCoinOnPedestal().getItem();
                if(coinInPed instanceof IUpgradeBase)
                {
                    impTicker++;
                    ((IUpgradeBase) coinInPed).updateAction(world,this);
                    //Has to be bigger than our biggest ticker value for an upgrade, or itll reset the upgrade instance before the upgrade action can fire
                    if(impTicker >=Integer.MAX_VALUE-100){impTicker=0;}
                }
            }
        }
        //Check at the source if the particles have been turned off???
        if(world.isRemote && !hasParticleDiffuser())
        {
            if(hasCoin())
            {
                Item coinInPed = getCoinOnPedestal().getItem();
                if(coinInPed instanceof IUpgradeBase)
                {
                    partTicker++;
                    Random rand = new Random();
                    ((IUpgradeBase) coinInPed).onRandomDisplayTick(this,partTicker, world.getBlockState(getPos()), world, getPos(), rand);
                    if(partTicker >=Integer.MAX_VALUE-100){partTicker=0;}
                }
            }
        }
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        CompoundNBT invTag = nbt.getCompound("inv");
        CompoundNBT invTagP = nbt.getCompound("invp");
        handler.ifPresent(h -> ((INBTSerializable<CompoundNBT>) h).deserializeNBT(invTag));
        privateHandler.ifPresent(ph -> ((INBTSerializable<CompoundNBT>) ph).deserializeNBT(invTagP));

        this.storedValueForUpgrades = nbt.getInt("storedUpgradeValue");
        this.boolLight = nbt.getBoolean("boollight");

        int[] storedIX = nbt.getIntArray("intArrayXPos");
        int[] storedIY = nbt.getIntArray("intArrayYPos");
        int[] storedIZ = nbt.getIntArray("intArrayZPos");

        for(int i=0;i<storedIX.length;i++)
        {
            BlockPos gotPos = new BlockPos(storedIX[i],storedIY[i],storedIZ[i]);
            storedLocations.add(gotPos);
        }

        this.lockCode = LockCode.read(nbt);
    }

    public CompoundNBT write(CompoundNBT tag) {
        super.write(tag);
        handler.ifPresent(h -> {
            CompoundNBT compound = ((INBTSerializable<CompoundNBT>) h).serializeNBT();
            tag.put("inv", compound);
        });

        privateHandler.ifPresent(ph -> {
            CompoundNBT compound = ((INBTSerializable<CompoundNBT>) ph).serializeNBT();
            tag.put("invp", compound);
        });

        tag.putInt("storedUpgradeValue",storedValueForUpgrades);
        tag.putBoolean("boollight", boolLight);

        List<Integer> storedX = new ArrayList<Integer>();
        List<Integer> storedY = new ArrayList<Integer>();
        List<Integer> storedZ = new ArrayList<Integer>();

        for(int i=0;i<getNumberOfStoredLocations();i++)
        {
            storedX.add(storedLocations.get(i).getX());
            storedY.add(storedLocations.get(i).getY());
            storedZ.add(storedLocations.get(i).getZ());
        }

        tag.putIntArray("intArrayXPos",storedX);
        tag.putIntArray("intArrayYPos",storedY);
        tag.putIntArray("intArrayZPos",storedZ);


        this.lockCode.write(tag);
        return tag;
    }

    // TODO: When syncing data to the client, only update the two itemsstacks that get updated, not everything.
    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        write(nbtTagCompound);
        int tileEntityType = 42;  // arbitrary number; only used for vanilla TileEntities.  You can use it, or not, as you want.
        return new SUpdateTileEntityPacket(this.pos, tileEntityType, nbtTagCompound);
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return write(new CompoundNBT());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        super.onDataPacket(net, pkt);
        BlockState state = this.world.getBlockState(this.pos);
        this.handleUpdateTag(state,pkt.getNbtCompound());
        this.world.notifyBlockUpdate(this.pos, state, state, 3);
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag)
    {
        this.read(state, tag);
    }

    private static Block[] pedArray = new Block[]{PedestalBlock.PEDESTAL_000, PedestalBlock.PEDESTAL_001, PedestalBlock.PEDESTAL_002, PedestalBlock.PEDESTAL_003
            , PedestalBlock.PEDESTAL_010, PedestalBlock.PEDESTAL_011, PedestalBlock.PEDESTAL_012, PedestalBlock.PEDESTAL_013
            , PedestalBlock.PEDESTAL_020, PedestalBlock.PEDESTAL_021, PedestalBlock.PEDESTAL_022, PedestalBlock.PEDESTAL_023
            , PedestalBlock.PEDESTAL_030, PedestalBlock.PEDESTAL_031, PedestalBlock.PEDESTAL_032, PedestalBlock.PEDESTAL_033
            , PedestalBlock.PEDESTAL_100, PedestalBlock.PEDESTAL_101, PedestalBlock.PEDESTAL_102, PedestalBlock.PEDESTAL_103
            , PedestalBlock.PEDESTAL_110, PedestalBlock.PEDESTAL_111, PedestalBlock.PEDESTAL_112, PedestalBlock.PEDESTAL_113
            , PedestalBlock.PEDESTAL_120, PedestalBlock.PEDESTAL_121, PedestalBlock.PEDESTAL_122, PedestalBlock.PEDESTAL_123
            , PedestalBlock.PEDESTAL_130, PedestalBlock.PEDESTAL_131, PedestalBlock.PEDESTAL_132, PedestalBlock.PEDESTAL_133
            , PedestalBlock.PEDESTAL_200, PedestalBlock.PEDESTAL_201, PedestalBlock.PEDESTAL_202, PedestalBlock.PEDESTAL_203
            , PedestalBlock.PEDESTAL_210, PedestalBlock.PEDESTAL_211, PedestalBlock.PEDESTAL_212, PedestalBlock.PEDESTAL_213
            , PedestalBlock.PEDESTAL_220, PedestalBlock.PEDESTAL_221, PedestalBlock.PEDESTAL_222, PedestalBlock.PEDESTAL_223
            , PedestalBlock.PEDESTAL_230, PedestalBlock.PEDESTAL_231, PedestalBlock.PEDESTAL_232, PedestalBlock.PEDESTAL_233
            , PedestalBlock.PEDESTAL_300, PedestalBlock.PEDESTAL_301, PedestalBlock.PEDESTAL_302, PedestalBlock.PEDESTAL_303
            , PedestalBlock.PEDESTAL_310, PedestalBlock.PEDESTAL_311, PedestalBlock.PEDESTAL_312, PedestalBlock.PEDESTAL_313
            , PedestalBlock.PEDESTAL_320, PedestalBlock.PEDESTAL_321, PedestalBlock.PEDESTAL_322, PedestalBlock.PEDESTAL_323
            , PedestalBlock.PEDESTAL_330, PedestalBlock.PEDESTAL_331, PedestalBlock.PEDESTAL_332, PedestalBlock.PEDESTAL_333};

    private static final ResourceLocation RESLOC_TILE_PEDESTAL = new ResourceLocation(MODID, "tile/pedestal");

    public static TileEntityType<PedestalTileEntity> PEDESTALTYPE = TileEntityType.Builder.create(PedestalTileEntity::new, pedArray).build(null);

    @SubscribeEvent
    public static void onTileEntityRegistry(final RegistryEvent.Register<TileEntityType<?>> event) {
        IForgeRegistry<TileEntityType<?>> r = event.getRegistry();
        r.register(PEDESTALTYPE.setRegistryName(RESLOC_TILE_PEDESTAL));
    }

    @Override
    public void remove() {
        super.remove();
        if(this.handler != null) {
            this.handler.invalidate();
        }
        if(this.privateHandler != null) {
            this.privateHandler.invalidate();
        }
        if(this.energyHandler != null) {
            this.energyHandler.invalidate();
        }
        if(this.fluidHandler != null) {
            this.fluidHandler.invalidate();
        }
    }
}
