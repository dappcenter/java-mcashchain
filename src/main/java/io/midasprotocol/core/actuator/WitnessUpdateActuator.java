package io.midasprotocol.core.actuator;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.midasprotocol.core.Wallet;
import io.midasprotocol.core.capsule.TransactionResultCapsule;
import io.midasprotocol.core.capsule.WitnessCapsule;
import io.midasprotocol.core.capsule.utils.TransactionUtil;
import io.midasprotocol.core.db.Manager;
import io.midasprotocol.core.exception.ContractExeException;
import io.midasprotocol.core.exception.ContractValidateException;
import io.midasprotocol.protos.Contract.WitnessUpdateContract;
import io.midasprotocol.protos.Protocol.Transaction.Result.Code;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "actuator")
public class WitnessUpdateActuator extends AbstractActuator {

	WitnessUpdateActuator(final Any contract, final Manager dbManager) {
		super(contract, dbManager);
	}

	private void updateWitness(final WitnessUpdateContract contract) {
		WitnessCapsule witnessCapsule = this.dbManager.getWitnessStore()
			.get(contract.getWitnessAddress().toByteArray());
		witnessCapsule.setUrl(contract.getUpdateUrl().toStringUtf8());
		this.dbManager.getWitnessStore().put(witnessCapsule.createDbKey(), witnessCapsule);
	}

	@Override
	public boolean execute(TransactionResultCapsule ret) throws ContractExeException {
		long fee = calcFee();
		try {
			final WitnessUpdateContract witnessUpdateContract = this.contract
				.unpack(WitnessUpdateContract.class);
			this.updateWitness(witnessUpdateContract);
			ret.setStatus(fee, Code.SUCCESS);
		} catch (final InvalidProtocolBufferException e) {
			logger.debug(e.getMessage(), e);
			ret.setStatus(fee, Code.FAILED);
			throw new ContractExeException(e.getMessage());
		}
		return true;
	}

	@Override
	public boolean validate() throws ContractValidateException {
		if (this.contract == null) {
			throw new ContractValidateException("No contract!");
		}
		if (this.dbManager == null) {
			throw new ContractValidateException("No dbManager!");
		}
		if (!this.contract.is(WitnessUpdateContract.class)) {
			throw new ContractValidateException(
				"contract type error, expected WitnessUpdateContract, actual " + contract.getClass());
		}
		final WitnessUpdateContract contract;
		try {
			contract = this.contract.unpack(WitnessUpdateContract.class);
		} catch (InvalidProtocolBufferException e) {
			logger.debug(e.getMessage(), e);
			throw new ContractValidateException(e.getMessage());
		}

		byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
		if (!Wallet.addressValid(ownerAddress)) {
			throw new ContractValidateException("Invalid ownerAddress");
		}

		if (!this.dbManager.getAccountStore().has(ownerAddress)) {
			throw new ContractValidateException("Owner account does not exist");
		}

		if (!TransactionUtil.validUrl(contract.getUpdateUrl().toByteArray())) {
			throw new ContractValidateException("Invalid url");
		}

		byte[] supernodeAddress = contract.getWitnessAddress().toByteArray();
		if (!Wallet.addressValid(supernodeAddress)) {
			throw new ContractValidateException("Invalid supernodeAddress");
		}

		if (!this.dbManager.getWitnessStore().has(supernodeAddress)) {
			throw new ContractValidateException("Witness does not exist");
		}

		WitnessCapsule witnessCapsule = this.dbManager.getWitnessStore().get(supernodeAddress);
		if (!ByteString.copyFrom(ownerAddress).equals(witnessCapsule.getOwnerAddress())) {
			throw new ContractValidateException("Account does not own supernode");
		}

		return true;
	}

	@Override
	public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
		return contract.unpack(WitnessUpdateContract.class).getOwnerAddress();
	}

	@Override
	public long calcFee() {
		return 0;
	}
}
