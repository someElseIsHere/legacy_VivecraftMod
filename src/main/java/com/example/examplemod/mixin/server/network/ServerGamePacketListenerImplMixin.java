package com.example.examplemod.mixin.server.network;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.api.AimFixHandler;
import org.vivecraft.api.NetworkHelper;
import org.vivecraft.api.ServerVivePlayer;

import com.example.examplemod.DataHolder;

import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Pose;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin implements ServerPlayerConnection, ServerGamePacketListener{
	
	@Shadow
	@Final
	public Connection connection;
	
	@Shadow
	public ServerPlayer player;

	@Shadow
	private int aboveGroundTickCount;
	
	
	@Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V")
	public void init(MinecraftServer p_9770_, Connection p_9771_, ServerPlayer p_9772_, CallbackInfo info) {
		// Vivecraft
		this.connection.channel.pipeline().addBefore("packet_handler", "vr_aim_fix",
				new AimFixHandler(this.connection));
	}
	
	@Inject(at = @At("TAIL"), method = "tick()V")
	public void posdata(CallbackInfo info) {
		NetworkHelper.sendPosData(this.player);
	}
	
	@Inject(at = @At("TAIL"), method = "handleCustomPayload(Lnet/minecraft/network/protocol/game/ServerboundCustomPayloadPacket;)V" )
	public void custompacket(ServerboundCustomPayloadPacket pPacket, CallbackInfo info) {
		FriendlyByteBuf friendlybytebuf = pPacket.getData();
		ResourceLocation resourcelocation = pPacket.getIdentifier();
		String s = resourcelocation.getNamespace();
		String s1 = resourcelocation.getPath();
		
		if (s.equalsIgnoreCase("vivecraft") && s1.equalsIgnoreCase("data"))
		{
			int i = friendlybytebuf.readableBytes();
			NetworkHelper.PacketDiscriminators networkhelper$packetdiscriminators = NetworkHelper.PacketDiscriminators.values()[friendlybytebuf.readByte()];
			byte[] abyte = new byte[i - 1];
			friendlybytebuf.readBytes(abyte);
			ServerVivePlayer serverviveplayer = NetworkHelper.vivePlayers.get(this.player.getUUID());
			
			if (serverviveplayer == null && networkhelper$packetdiscriminators != NetworkHelper.PacketDiscriminators.VERSION)
			{
				return;
			}
			
			switch (networkhelper$packetdiscriminators)
			{
			case VERSION:
				String s2 = DataHolder.getInstance().minecriftVerString;
				this.send(NetworkHelper.getVivecraftServerPacket(NetworkHelper.PacketDiscriminators.VERSION, s2));
				this.send(NetworkHelper.getVivecraftServerPacket(NetworkHelper.PacketDiscriminators.REQUESTDATA, new byte[0]));
				this.send(NetworkHelper.getVivecraftServerPacket(NetworkHelper.PacketDiscriminators.CLIMBING, new byte[] {1, 0}));
				this.send(NetworkHelper.getVivecraftServerPacket(NetworkHelper.PacketDiscriminators.TELEPORT, new byte[0]));
				this.send(NetworkHelper.getVivecraftServerPacket(NetworkHelper.PacketDiscriminators.CRAWL, new byte[0]));
				serverviveplayer = new ServerVivePlayer(this.player);
				NetworkHelper.vivePlayers.put(this.player.getUUID(), serverviveplayer);
				BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new DataInputStream(new ByteArrayInputStream(abyte))));
				
				try
				{
					String s3 = bufferedreader.readLine();
					
					if (s3.contains("NONVR"))
					{
						this.player.sendMessage(new TextComponent("NONVR: " + this.player.getDisplayName().getString()), this.player.getUUID());
						serverviveplayer.setVR(false);
					}
					else
					{
						this.player.sendMessage(new TextComponent("VR: " + this.player.getDisplayName().getString()), this.player.getUUID());
						serverviveplayer.setVR(true);
					}
				}
				catch (IOException ioexception)
				{
					ioexception.printStackTrace();
				}
				
				break;
				
			case CONTROLLER0DATA:
				serverviveplayer.controller0data = abyte;
				break;
				
			case CONTROLLER1DATA:
				serverviveplayer.controller1data = abyte;
				break;
				
			case DRAW:
				serverviveplayer.draw = abyte;
				break;
				
			case HEADDATA:
				serverviveplayer.hmdData = abyte;
				
			case MOVEMODE:
			case REQUESTDATA:
			default:
				break;
				
			case WORLDSCALE:
				friendlybytebuf.resetReaderIndex();
				friendlybytebuf.readByte();
				serverviveplayer.worldScale = friendlybytebuf.readFloat();
				break;
			case HEIGHT:
				friendlybytebuf.resetReaderIndex();
				friendlybytebuf.readByte();
				serverviveplayer.heightscale = friendlybytebuf.readFloat();
				break;
				
			case TELEPORT:
				friendlybytebuf.resetReaderIndex();
				friendlybytebuf.readByte();
				float f = friendlybytebuf.readFloat();
				float f1 = friendlybytebuf.readFloat();
				float f2 = friendlybytebuf.readFloat();
				this.player.absMoveTo((double)f, (double)f1, (double)f2, this.player.getYRot(), this.player.getXRot());
				break;
				
			case CLIMBING:
				this.player.fallDistance = 0.0F;
				this.aboveGroundTickCount = 0; //why were we not doing this
			case ACTIVEHAND:
				friendlybytebuf.resetReaderIndex();
				friendlybytebuf.readByte();
				serverviveplayer.activeHand = friendlybytebuf.readByte();
				
				if (serverviveplayer.isSeated())
				{
					serverviveplayer.activeHand = 0;
				}
				
				break;
				
			case CRAWL:
				friendlybytebuf.resetReaderIndex();
				friendlybytebuf.readByte();
				serverviveplayer.crawling = friendlybytebuf.readByte() != 0;
				
				if (serverviveplayer.crawling)
				{
					this.player.setPose(Pose.SWIMMING);
				}
			}
		}
	}
}
