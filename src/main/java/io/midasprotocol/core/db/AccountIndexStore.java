package io.midasprotocol.core.db;

import com.google.protobuf.ByteString;
import io.midasprotocol.core.capsule.AccountCapsule;
import io.midasprotocol.core.capsule.BytesCapsule;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AccountIndexStore extends TronStoreWithRevoking<BytesCapsule> {

	@Autowired
	public AccountIndexStore(@Value("account-index") String dbName) {
		super(dbName);
	}

	public void put(AccountCapsule accountCapsule) {
		put(accountCapsule.getAccountName().toByteArray(),
			new BytesCapsule(accountCapsule.getAddress().toByteArray()));
	}

	public byte[] get(ByteString name) {
		BytesCapsule bytesCapsule = get(name.toByteArray());
		if (Objects.nonNull(bytesCapsule)) {
			return bytesCapsule.getData();
		}
		return null;
	}

	@Override
	public BytesCapsule get(byte[] key) {
		byte[] value = revokingDB.getUnchecked(key);
		if (ArrayUtils.isEmpty(value)) {
			return null;
		}
		return new BytesCapsule(value);
	}

	@Override
	public boolean has(byte[] key) {
		byte[] value = revokingDB.getUnchecked(key);
		return !ArrayUtils.isEmpty(value);
	}
}