package stest.tron.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.midasprotocol.api.WalletGrpc;
import io.midasprotocol.api.WalletSolidityGrpc;
import io.midasprotocol.common.crypto.ECKey;
import io.midasprotocol.common.utils.ByteArray;
import io.midasprotocol.common.utils.Utils;
import io.midasprotocol.core.Wallet;
import io.midasprotocol.protos.Protocol.Account;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethed;

import java.util.concurrent.TimeUnit;

@Slf4j
public class WalletTestAccount014 {

	private final String testKey002 = Configuration.getByPath("testng.conf")
		.getString("foundationAccount.key2");
	private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
	ECKey ecKey1 = new ECKey(Utils.getRandom());
	byte[] account014Address = ecKey1.getAddress();
	String account014Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
	ECKey ecKey2 = new ECKey(Utils.getRandom());
	byte[] account014SecondAddress = ecKey2.getAddress();
	String account014SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
	private ManagedChannel channelFull = null;
	private ManagedChannel channelSolidity = null;
	private ManagedChannel channelSoliInFull = null;
	private WalletGrpc.WalletBlockingStub blockingStubFull = null;
	private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
	private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
	private Long maxFeeLimit = Configuration.getByPath("testng.conf")
		.getLong("defaultParameter.maxFeeLimit");
	private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
		.get(0);
	private String soliditynode = Configuration.getByPath("testng.conf")
		.getStringList("solidityNode.ip.list").get(0);
	private String soliInFullnode = Configuration.getByPath("testng.conf")
		.getStringList("solidityNode.ip.list").get(1);

	@BeforeSuite
	public void beforeSuite() {
		Wallet wallet = new Wallet();
	}

	/**
	 * constructor.
	 */
	@BeforeClass(enabled = true)
	public void beforeClass() {
		PublicMethed.printAddress(testKey002);
		channelFull = ManagedChannelBuilder.forTarget(fullnode)
			.usePlaintext(true)
			.build();
		blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

		channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
			.usePlaintext(true)
			.build();
		blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

		channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
			.usePlaintext(true)
			.build();
		blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);
	}

	@Test(enabled = true, description = "Query freeNetUsage in 50061")
	public void fullAndSoliMerged1ForFreeNetUsage() {
		//Create account014
		ecKey1 = new ECKey(Utils.getRandom());
		account014Address = ecKey1.getAddress();
		account014Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
		ecKey2 = new ECKey(Utils.getRandom());
		account014SecondAddress = ecKey2.getAddress();
		account014SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

		PublicMethed.printAddress(account014Key);
		PublicMethed.printAddress(account014SecondKey);
		Assert.assertTrue(PublicMethed.sendcoin(account014Address, 1000000000L, fromAddress,
			testKey002, blockingStubFull));

		//Test freeNetUsage in fullnode and soliditynode.
		Assert.assertTrue(PublicMethed.sendcoin(account014SecondAddress, 5000000L,
			account014Address, account014Key,
			blockingStubFull));
		Assert.assertTrue(PublicMethed.sendcoin(account014SecondAddress, 5000000L,
			account014Address, account014Key,
			blockingStubFull));
		PublicMethed.waitProduceNextBlock(blockingStubFull);
		Account account014 = PublicMethed.queryAccount(account014Address, blockingStubFull);
		final long freeNetUsageInFullnode = account014.getAccountResource().getFreeBandwidthUsage();
		final long createTimeInFullnode = account014.getCreateTime();
		final long lastOperationTimeInFullnode = account014.getLatestOperationTime();
		final long lastCustomeFreeTimeInFullnode = account014.getAccountResource().getLatestFreeBandwidthConsumeTime();
		PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
		account014 = PublicMethed.queryAccount(account014Address, blockingStubSoliInFull);
		final long freeNetUsageInSoliInFull = account014.getAccountResource().getFreeBandwidthUsage();
		final long createTimeInSoliInFull = account014.getCreateTime();
		final long lastOperationTimeInSoliInFull = account014.getLatestOperationTime();
		final long lastCustomeFreeTimeInSoliInFull = account014.getAccountResource().getLatestFreeBandwidthConsumeTime();
		//PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);
		account014 = PublicMethed.queryAccount(account014Address, blockingStubSolidity);
		final long freeNetUsageInSolidity = account014.getAccountResource().getFreeBandwidthUsage();
		final long createTimeInSolidity = account014.getCreateTime();
		final long lastOperationTimeInSolidity = account014.getLatestOperationTime();
		final long lastCustomeFreeTimeInSolidity = account014.getAccountResource().getLatestFreeBandwidthConsumeTime();
		Assert.assertTrue(freeNetUsageInSoliInFull > 0 && freeNetUsageInSolidity > 0
			&& freeNetUsageInFullnode > 0);
		Assert.assertTrue(freeNetUsageInFullnode <= freeNetUsageInSoliInFull + 5
			&& freeNetUsageInFullnode >= freeNetUsageInSoliInFull - 5);
		Assert.assertTrue(freeNetUsageInFullnode <= freeNetUsageInSolidity + 5
			&& freeNetUsageInFullnode >= freeNetUsageInSolidity - 5);
		Assert.assertTrue(createTimeInFullnode == createTimeInSolidity && createTimeInFullnode
			== createTimeInSoliInFull);
		Assert.assertTrue(createTimeInSoliInFull != 0);
		Assert.assertTrue(lastOperationTimeInFullnode == lastOperationTimeInSolidity
			&& lastOperationTimeInFullnode == lastOperationTimeInSoliInFull);
		Assert.assertTrue(lastOperationTimeInSoliInFull != 0);
		Assert.assertTrue(lastCustomeFreeTimeInFullnode == lastCustomeFreeTimeInSolidity
			&& lastCustomeFreeTimeInFullnode == lastCustomeFreeTimeInSoliInFull);
		Assert.assertTrue(lastCustomeFreeTimeInSoliInFull != 0);
	}

	@Test(enabled = true, description = "Query net usage in 50061")
	public void fullAndSoliMerged2ForNetUsage() {

		Assert.assertTrue(PublicMethed.freezeBalance(account014Address, 1000000L, 3,
			account014Key, blockingStubFull));
		Assert.assertTrue(PublicMethed.sendcoin(account014SecondAddress, 1000000L,
			account014Address, account014Key, blockingStubFull));
		Assert.assertTrue(PublicMethed.freezeBalanceGetEnergy(account014Address, 1000000,
			3, 1, account014Key, blockingStubFull));
		Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(account014Address, 1000000,
			3, 0, ByteString.copyFrom(
				account014SecondAddress), account014Key, blockingStubFull));
		Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(account014Address, 1000000,
			3, 1, ByteString.copyFrom(
				account014SecondAddress), account014Key, blockingStubFull));
		Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(account014SecondAddress, 1000000,
			3, 0, ByteString.copyFrom(
				account014Address), account014SecondKey, blockingStubFull));
		Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(account014SecondAddress, 1000000,
			3, 1, ByteString.copyFrom(
				account014Address), account014SecondKey, blockingStubFull));

		PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
		Account account014 = PublicMethed.queryAccount(account014Address, blockingStubFull);
		final long lastCustomeTimeInFullnode = account014.getAccountResource().getLatestBandwidthConsumeTime();
		final long netUsageInFullnode = account014.getAccountResource().getBandwidthUsage();
		final long acquiredForBandwidthInFullnode = account014
			.getDelegatedFrozenForBandwidth().getAcquiredDelegatedBalance();
		final long delegatedBandwidthInFullnode = account014
			.getDelegatedFrozenForBandwidth().getDelegatedBalance();
		final long acquiredForEnergyInFullnode = account014
			.getDelegatedFrozenForEnergy().getAcquiredDelegatedBalance();
		final long delegatedForEnergyInFullnode = account014
			.getDelegatedFrozenForEnergy().getDelegatedBalance();
		logger.info("delegatedForEnergyInFullnode " + delegatedForEnergyInFullnode);
		PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
		account014 = PublicMethed.queryAccount(account014Address, blockingStubSoliInFull);
		final long lastCustomeTimeInSoliInFull = account014.getAccountResource().getLatestBandwidthConsumeTime();
		logger.info("freeNetUsageInSoliInFull " + lastCustomeTimeInSoliInFull);
		final long netUsageInSoliInFull = account014.getAccountResource().getBandwidthUsage();
		final long acquiredForBandwidthInSoliInFull = account014
			.getDelegatedFrozenForBandwidth().getAcquiredDelegatedBalance();
		final long delegatedBandwidthInSoliInFull = account014
			.getDelegatedFrozenForBandwidth().getDelegatedBalance();
		final long acquiredForEnergyInSoliInFull = account014
			.getDelegatedFrozenForEnergy().getAcquiredDelegatedBalance();
		final long delegatedForEnergyInSoliInFull = account014
			.getDelegatedFrozenForEnergy().getDelegatedBalance();
		logger.info("delegatedForEnergyInSoliInFull " + delegatedForEnergyInSoliInFull);
		//PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);
		account014 = PublicMethed.queryAccount(account014Address, blockingStubSolidity);
		final long netUsageInSolidity = account014.getAccountResource().getBandwidthUsage();
		final long lastCustomeTimeInSolidity = account014.getAccountResource().getLatestBandwidthConsumeTime();
		final long acquiredForBandwidthInSolidity = account014
			.getDelegatedFrozenForBandwidth().getAcquiredDelegatedBalance();
		final long delegatedBandwidthInSolidity = account014
			.getDelegatedFrozenForBandwidth().getDelegatedBalance();
		final long acquiredForEnergyInSolidity = account014
			.getDelegatedFrozenForEnergy().getAcquiredDelegatedBalance();
		final long delegatedForEnergyInSolidity = account014
			.getDelegatedFrozenForEnergy().getDelegatedBalance();

		logger.info("delegatedForEnergyInSolidity " + delegatedForEnergyInSolidity);
		Assert.assertTrue(netUsageInSoliInFull > 0 && netUsageInSolidity > 0
			&& netUsageInFullnode > 0);
		Assert.assertTrue(netUsageInFullnode <= netUsageInSoliInFull + 5
			&& netUsageInFullnode >= netUsageInSoliInFull - 5);
		Assert.assertTrue(netUsageInFullnode <= netUsageInSolidity + 5
			&& netUsageInFullnode >= netUsageInSolidity - 5);
		Assert.assertTrue(acquiredForBandwidthInFullnode == acquiredForBandwidthInSoliInFull
			&& acquiredForBandwidthInFullnode == acquiredForBandwidthInSolidity);
		Assert.assertTrue(delegatedBandwidthInFullnode == delegatedBandwidthInSoliInFull
			&& delegatedBandwidthInFullnode == delegatedBandwidthInSolidity);
		Assert.assertTrue(acquiredForEnergyInFullnode == acquiredForEnergyInSoliInFull
			&& acquiredForEnergyInFullnode == acquiredForEnergyInSolidity);
		Assert.assertTrue(delegatedForEnergyInFullnode == delegatedForEnergyInSoliInFull
			&& delegatedForEnergyInFullnode == delegatedForEnergyInSolidity);
		Assert.assertTrue(acquiredForBandwidthInSoliInFull == 1000000
			&& delegatedBandwidthInSoliInFull == 1000000 && acquiredForEnergyInSoliInFull == 1000000
			&& delegatedForEnergyInSoliInFull == 1000000);
		logger.info("lastCustomeTimeInSoliInFull " + lastCustomeTimeInSoliInFull);
		Assert.assertTrue(lastCustomeTimeInFullnode == lastCustomeTimeInSolidity
			&& lastCustomeTimeInFullnode == lastCustomeTimeInSoliInFull);
		logger.info("lastCustomeTimeInSoliInFull " + lastCustomeTimeInSoliInFull);
		Assert.assertTrue(lastCustomeTimeInSoliInFull != 0);

	}

	/**
	 * constructor.
	 */
	@AfterClass
	public void shutdown() throws InterruptedException {
		if (channelFull != null) {
			channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
		}
	}
}