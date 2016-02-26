package com.dipcoin.recon.delegate;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.DipcoinTransaction;

@Component("reconciliationDelegate")
public class ReconciliationDelegateHelper implements ReconciliationDelegate {

	@Autowired
	ApplicationContext appContext;
	
	@Autowired
	private DipcoinDBService dipcoinDBService;
	
	@Autowired
	private BankDBService bankDBService;
	
	public static final char GROUP_SEPARATOR = '\u241D';
	DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	NumberFormat formatter = new DecimalFormat("#0.##");
	
	/**
	 * @param merchantId the merchantid
	 * @return true if the reconciliation is performed successfully
	 */
	public boolean performMerchantReconciliation(Integer merchantId) throws Exception{
		try {
			List<DipcoinTransaction> dipcoinTransactionsList = getDipcoinTransactionsForMerchant(0); // TODO Remove hard-coding
//			Resource resource = appContext
//					.getResource("file:C:\\Tushar\\Work\\Projects\\dipcoin\\merchanttranfile.xlsx");
//
//			FileChannel inChannel = new RandomAccessFile(resource.getFile(), "r").getChannel();
//			InputStream inputStream = Channels.newInputStream(inChannel);
//			int index = 0;
//			InputStream inputStream = resource.getInputStream();
			InputStream inputStream = retrieveFile();
			Workbook workbook = new XSSFWorkbook(inputStream);
			Sheet firstSheet = workbook.getSheetAt(0);
			Iterator<Row> iterator = firstSheet.iterator();
			int matchCount = 0, misMatchCount = 0;
			while (iterator.hasNext()) {
				Row nextRow = iterator.next();
				StringBuilder line = new StringBuilder();
				if (nextRow.getRowNum() > 0) {
					Iterator<Cell> cellIterator = nextRow.cellIterator();
					while (cellIterator.hasNext()) {
						Cell cell = cellIterator.next();

						switch (cell.getCellType()) {
						case Cell.CELL_TYPE_STRING:
							line.append(cell.getStringCellValue());
							break;
						case Cell.CELL_TYPE_BOOLEAN:
							line.append(cell.getBooleanCellValue());
							break;
						case Cell.CELL_TYPE_NUMERIC:
							line.append(new BigDecimal(cell.getNumericCellValue()));
							break;
						}
						line.append(",");
					}
//					System.out.println(" merchant Line : "+ line.toString());
					String[] tokens = StringUtils.split(line.toString(), ',');
					String merchantTransString = getComparisonKeyForMerchant(tokens);

					for (DipcoinTransaction dipcoinTransaction : dipcoinTransactionsList) {
//						++index;
						String comparisonKeyDipcoin = getComparisonKeyForDipcoin(dipcoinTransaction);
						if (merchantTransString.equalsIgnoreCase(comparisonKeyDipcoin)) {
							++matchCount;
							if (dipcoinTransaction.getDCResponseDesc().equalsIgnoreCase(tokens[tokens.length-1])) {
								System.out.println("both success");
							}else{
								System.out.println("recon required");
							}
//							System.out.println("matched tokens dipcoin status : "+dipcoinTransaction.getDCResponseDesc());
//							System.out.println("matched tokens merchant status : "+ tokens[tokens.length-1]);
						}
						// else{ TODO to add code for mismatched transactions
						// ++misMatchCount;
						// }
					}
					// System.out.println(merchantTransString);
				}
			}
			System.out.println("matched Count:" + matchCount);
			System.out.println("mis matched Count:" + misMatchCount);
			workbook.close();
			inputStream.close();
//			inChannel.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * @param merchantId the merchantid
	 * @return List<DipcoinTransaction> the list of Transactions for the passed merchantId
	 */
	public List<DipcoinTransaction> getAllDipcoinTransactionsForMerchantId(Integer merchantId) {
		List<DipcoinTransaction> transactionsList = new ArrayList<>();
		try {
			transactionsList = dipcoinDBService.getAllDipcoinTransactionsForMerchantId(merchantId).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("transactions List = " + transactionsList);
		return transactionsList;
	}
	
	/**
	 * @param merchantId the merchantid
	 * @return List<DipcoinTransaction> the list of Transactions for the passed merchantId
	 */
	public List<DipcoinTransaction> getDipcoinTransactionsForMerchant(int merchantId) {
		List<DipcoinTransaction> dcTransactions = new ArrayList<>();

		try {
			Future<List<DipcoinTransaction>> asyncResults = dipcoinDBService.getAllDipcoinTransactionsForMerchantId(merchantId);
			dcTransactions = asyncResults.get();
			System.out.println("transactions : " + dcTransactions);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dcTransactions;
	}
	
	/**
	 * @param dipcoinTransactionId the dipcoin transaction id
	 * @return bankTransaction the bank Transaction for the passed dipcoin Transaction Id
	 */
	public BankTransaction getBankTransactionForDCTransactionId(Integer dcTransactionId){
		BankTransaction bankTransaction = new BankTransaction();
		try {
			Future<BankTransaction> asyncResults = bankDBService.getTransactionForId(dcTransactionId);
			bankTransaction = asyncResults.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bankTransaction;
	}
	
	private String getComparisonKeyForDipcoin(DipcoinTransaction dipcoinTransaction) {
		StringBuilder sb = new StringBuilder("");
		sb.append(dipcoinTransaction.getDipcoinTransactionId()).append(GROUP_SEPARATOR)
				.append(formatter.format(dipcoinTransaction.getAmount())).append(GROUP_SEPARATOR)
				.append(sdf.format(dipcoinTransaction.getResponseTime()));
		// System.out.println("dc tran key :"+ sb.toString());
		return sb.toString();
	}
	
	private String getComparisonKeyForMerchant(String[] tokens) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.trimToEmpty(tokens[1])).append(GROUP_SEPARATOR).append(StringUtils.trimToEmpty(tokens[2]))
				.append(GROUP_SEPARATOR).append(StringUtils.trimToEmpty(tokens[3]));
		return sb.toString();
	}

	@Override
	public void performBankReconciliation(Integer bankId) throws Exception {
		System.out.println("Bank ReconciliationCalled--- "+new java.util.Date());
		
	}
	
	public InputStream retrieveFile(){
		String serverAddress = "127.0.0.1"; // ftp server address
		int port = 21; // ftp uses default port Number 21
		String username = "User1";// username of ftp server
		String password = "password"; // password of ftp server

		FTPClient ftpClient = new FTPClient();
		InputStream ipstream = null; 
		try {

			ftpClient.connect(serverAddress, port);
			ftpClient.login(username, password);

			ftpClient.enterLocalPassiveMode();
//			ftpClient.setFileType(FTP.BINARY_FILE_TYPE / FTP.ASCII_FILE_TYPE);
			String remoteFilePath = "/PrivateLink/merchanttranfile.xlsx";
//			File localfile = new File("C:/Tushar/ftpServerFile.csv");
//			OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localfile));
//			boolean success = ftpClient.retrieveFile(remoteFilePath, outputStream);
//			outputStream.close();

			 ipstream = ftpClient.retrieveFileStream(remoteFilePath);
			System.out.println(ipstream);
			if (ipstream != null) {
				System.out.println("Ftp file successfully download.");
			}

		} catch (IOException ex) {
			System.out.println("Error occurs in downloading files from ftp Server : " + ex.getMessage());
		} finally {
			try {
				if (ftpClient.isConnected()) {
					ftpClient.logout();
					ftpClient.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return ipstream;
	}
}
