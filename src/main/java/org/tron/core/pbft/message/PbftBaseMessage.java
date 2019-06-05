package org.tron.core.pbft.message;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Arrays;
import org.spongycastle.util.encoders.Hex;
import org.tron.common.crypto.ECKey;
import org.tron.common.overlay.message.Message;
import org.tron.common.utils.Sha256Hash;
import org.tron.core.capsule.TransactionCapsule;
import org.tron.core.exception.P2pException;
import org.tron.protos.Protocol.PbftMessage;

public abstract class PbftBaseMessage extends Message {

  protected PbftMessage pbftMessage;

  public PbftBaseMessage() {
  }

  public PbftBaseMessage(byte type, byte[] data) throws IOException, P2pException {
    super(type, data);
    this.pbftMessage = PbftMessage.parseFrom(getCodedInputStream(data));
    if (isFilter()) {
      compareBytes(data, pbftMessage.toByteArray());
    }
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public PbftMessage getPbftMessage() {
    return pbftMessage;
  }

  public PbftBaseMessage setPbftMessage(PbftMessage pbftMessage) {
    this.pbftMessage = pbftMessage;
    return this;
  }

  public PbftBaseMessage setData(byte[] data) {
    this.data = data;
    return this;
  }

  public PbftBaseMessage setType(byte type) {
    this.type = type;
    return this;
  }

  public String getKey() {
    return getNo() + "_" + Hex.toHexString(pbftMessage.getRawData().getPublicKey().toByteArray());
  }

  public String getDataKey() {
    return getNo() + "_" + Hex.toHexString(pbftMessage.getRawData().getData().toByteArray());
  }

  public abstract String getNo();

  public boolean validateSignature(PbftBaseMessage pbftBaseMessage)
      throws SignatureException {
    byte[] hash = Sha256Hash.hash(pbftBaseMessage.getPbftMessage().getRawData().toByteArray());
    byte[] sigAddress = ECKey.signatureToAddress(hash, TransactionCapsule
        .getBase64FromByteString(pbftBaseMessage.getPbftMessage().getSign()));
    byte[] witnessAccountAddress = pbftBaseMessage.getPbftMessage().getRawData().getPublicKey()
        .toByteArray();
    return Arrays.equals(sigAddress, witnessAccountAddress);
  }

  @Override
  public String toString() {
    return "PbftMsgType:" + pbftMessage.getRawData().getPbftMsgType()
        + ", data:" + Hex.toHexString(pbftMessage.getRawData().getData().toByteArray())
        + ", " + super.toString();
  }
}