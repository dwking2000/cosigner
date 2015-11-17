package io.emax.cosigner.ethereum.common;

import io.emax.cosigner.common.ByteUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;

public class Rlp {
  private static final Logger LOGGER = LoggerFactory.getLogger(Rlp.class);
  private static final int THRESHOLD = 56;
  private static final int SHORT_ITEM = 0x80;
  private static final int LONG_ITEM = 0xb7;
  private static final int SHORT_LIST = 0xc0;
  private static final int LONG_LIST = 0xf7;

  /**
   * Parse a byte array into its RLP components.
   * 
   * @param input Byte-encoded RLP entity.
   * @return RLP data structure.
   */
  public static RlpEntity parseArray(byte[] input) {
    try {
      RlpEntity parsedEntity = null;
      int header = 0xFF & input[0];
      if (header >= SHORT_ITEM && header < LONG_ITEM) {
        int byteCount = header - SHORT_ITEM;
        parsedEntity = new RlpItem();
        parsedEntity.setEncodedContents(Arrays.copyOfRange(input, 0, 1 + byteCount));
        parsedEntity.setDecodedContents(Arrays.copyOfRange(input, 1, 1 + byteCount));
      } else if (header >= LONG_ITEM && header < SHORT_LIST) {
        int sizeBytes = header - LONG_ITEM;
        BigInteger byteCount = new BigInteger(1, Arrays.copyOfRange(input, 1, sizeBytes + 1));
        parsedEntity = new RlpItem();
        parsedEntity
            .setEncodedContents(Arrays.copyOfRange(input, 0, 1 + sizeBytes + byteCount.intValue()));
        parsedEntity.setDecodedContents(
            Arrays.copyOfRange(input, 1 + sizeBytes, 1 + sizeBytes + byteCount.intValue()));
      } else if (header >= SHORT_LIST && header < LONG_LIST) {
        int byteCount = header - SHORT_LIST;
        parsedEntity = new RlpList();
        parsedEntity.setEncodedContents(Arrays.copyOfRange(input, 0, 1 + byteCount));
        parsedEntity.setDecodedContents(Arrays.copyOfRange(input, 1, 1 + byteCount));
      } else if (header >= LONG_LIST) {
        int sizeBytes = header - LONG_LIST;
        BigInteger byteCount = new BigInteger(1, Arrays.copyOfRange(input, 1, sizeBytes + 1));
        parsedEntity = new RlpList();
        parsedEntity
            .setEncodedContents(Arrays.copyOfRange(input, 0, 1 + sizeBytes + byteCount.intValue()));
        parsedEntity.setDecodedContents(
            Arrays.copyOfRange(input, 1 + sizeBytes, 1 + sizeBytes + byteCount.intValue()));
      } else {
        parsedEntity = new RlpItem();
        parsedEntity.setEncodedContents(Arrays.copyOfRange(input, 0, 1));
        parsedEntity.setDecodedContents(Arrays.copyOfRange(input, 0, 1));
      }

      return parsedEntity;
    } catch (RuntimeException e) {
      LOGGER.warn(null, e);
      return null;
    }
  }

  /**
   * Encodes an RLP item into a byte array.
   * 
   * @param item RLP data that needs to be encoded.
   * @return Byte array with RLP information added.
   */
  public static byte[] encodeItem(byte[] item) {
    if (item.length == 1 && (int) (0xFF & item[0]) != 0 && (int) (0xFF & item[0]) < SHORT_ITEM) {
      return item;
    } else if (item.length < THRESHOLD) {
      byte[] encodedItem = new byte[1 + item.length];
      encodedItem[0] = (byte) (SHORT_ITEM + item.length);
      System.arraycopy(item, 0, encodedItem, 1, item.length);
      return encodedItem;
    } else {
      byte[] itemSize =
          ByteUtilities.stripLeadingNullBytes(new BigInteger(item.length + "").toByteArray());
      byte[] encodedItem = new byte[1 + itemSize.length + item.length];
      encodedItem[0] = (byte) (LONG_ITEM + itemSize.length);
      System.arraycopy(itemSize, 0, encodedItem, 1, itemSize.length);
      System.arraycopy(item, 0, encodedItem, 1 + itemSize.length, item.length);
      return encodedItem;
    }
  }

  /**
   * Encodes an RLP list into a byte array, including items inside the list.
   * 
   * @param list RLP data that needs to be encoded.
   * @return Byte array with RLP information added.
   */
  public static byte[] encodeList(LinkedList<RlpEntity> list) {
    byte[] encodedListData = new byte[0];
    for (RlpEntity item : list) {
      byte[] encodedItem = item.encode();
      byte[] encodedListMerger = new byte[encodedListData.length + encodedItem.length];
      System.arraycopy(encodedListData, 0, encodedListMerger, 0, encodedListData.length);
      System.arraycopy(encodedItem, 0, encodedListMerger, encodedListData.length,
          encodedItem.length);
      encodedListData = Arrays.copyOf(encodedListMerger, encodedListMerger.length);
    }

    if (encodedListData.length < THRESHOLD) {
      byte[] encodedItem = new byte[1 + encodedListData.length];
      encodedItem[0] = (byte) (SHORT_LIST + encodedListData.length);
      System.arraycopy(encodedListData, 0, encodedItem, 1, encodedListData.length);
      return encodedItem;
    } else {
      byte[] itemSize = ByteUtilities
          .stripLeadingNullBytes(new BigInteger(encodedListData.length + "").toByteArray());
      byte[] encodedItem = new byte[1 + itemSize.length + encodedListData.length];
      encodedItem[0] = (byte) (LONG_LIST + itemSize.length);
      System.arraycopy(itemSize, 0, encodedItem, 1, itemSize.length);
      System.arraycopy(encodedListData, 0, encodedItem, 1 + itemSize.length,
          encodedListData.length);
      return encodedItem;
    }
  }
}
