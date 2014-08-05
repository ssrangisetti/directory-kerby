package org.haox.kerb.crypto.enc.provider;

import org.haox.kerb.crypto.enc.EncryptProvider;
import org.haox.kerb.spec.KrbException;

public abstract class AbstractEncryptProvider implements EncryptProvider {
    private int blockSize;
    private int keyInputSize;
    private int keySize;

    public AbstractEncryptProvider(int blockSize, int keyInputSize, int keySize) {
        this.blockSize = blockSize;
        this.keyInputSize = keyInputSize;
        this.keySize = keySize;
    }

    @Override
    public int keyInputSize() {
        return keyInputSize;
    }

    @Override
    public int keySize() {
        return keySize;
    }

    @Override
    public int blockSize() {
        return blockSize;
    }

    @Override
    public byte[] initState(byte[] key, int keyUsage) {
        return new byte[0];
    }

    @Override
    public void encrypt(byte[] key, byte[] cipherState, byte[] data) throws KrbException {
        doEncrypt(data, key, cipherState, true);
    }

    @Override
    public void decrypt(byte[] key, byte[] cipherState, byte[] data) throws KrbException {
        doEncrypt(data, key, cipherState, false);
    }

    protected abstract void doEncrypt(byte[] data, byte[] key, byte[] cipherState, boolean encrypt) throws KrbException;

    @Override
    public void cbcMac(byte[] key, byte[] iv, byte[] data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cleanState() {

    }

    @Override
    public void cleanKey() {

    }
}
