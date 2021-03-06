/************************************************************************************************
 *  _________          _     ____          _           __        __    _ _      _   _   _ ___
 * |__  / ___|__ _ ___| |__ / ___|_      _(_)_ __   __ \ \      / /_ _| | | ___| |_| | | |_ _|
 *   / / |   / _` / __| '_ \\___ \ \ /\ / / | '_ \ / _` \ \ /\ / / _` | | |/ _ \ __| | | || |
 *  / /| |__| (_| \__ \ | | |___) \ V  V /| | | | | (_| |\ V  V / (_| | | |  __/ |_| |_| || |
 * /____\____\__,_|___/_| |_|____/ \_/\_/ |_|_| |_|\__, | \_/\_/ \__,_|_|_|\___|\__|\___/|___|
 *                                                 |___/
 *
 * Copyright (c) 2016-2017 Ivan Vaklinov <ivan@vaklinov.com>
 * Copyright (c) 2016-2017 David Mercer <radix42@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 **********************************************************************************/
package com.vaklinov.zcashui;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;


/**
 * Provides the functionality for sending cash
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class SendCashPanel
	extends WalletTabPanel
{
    
    private static final Logger LOG = Logger.getLogger(SendCashPanel.class.getName());
    
	private ZCashClientCaller clientCaller;
	private StatusUpdateErrorReporter errorReporter;
	
	private JComboBox  balanceAddressCombo     = null;
	private JPanel     comboBoxParentPanel     = null;
	private String[][] lastAddressBalanceData  = null;
	private String[]   comboBoxItems           = null;
	private DataGatheringThread<String[][]> addressBalanceGatheringThread = null;
	
	private JTextField destinationAddressField = null;
	private JTextField destinationAmountField  = null;
	private JTextField destinationMemoField    = null;	
	private JTextField transactionFeeField     = null;	
        private JTextField transactiondonationField = null;
	private JButton    sendButton              = null;
	
	private JPanel       operationStatusPanel        = null;
	private JLabel       operationStatusLabel        = null;
	private JProgressBar operationStatusProhgressBar = null;
	private Timer        operationStatusTimer        = null;
	private String       operationStatusID           = null;
	private int          operationStatusCounter      = 0;
        private int          nagged                      = 0;

	public SendCashPanel(ZCashClientCaller clientCaller,  StatusUpdateErrorReporter errorReporter)
		throws IOException, InterruptedException, WalletCallException
	{
		this.timers = new ArrayList<Timer>();
		this.threads = new ArrayList<DataGatheringThread<?>>();
		
		this.clientCaller = clientCaller;
		this.errorReporter = errorReporter;

		// Build content
		this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		this.setLayout(new BorderLayout());
		JPanel sendCashPanel = new JPanel();
		this.add(sendCashPanel, BorderLayout.NORTH);
		sendCashPanel.setLayout(new BoxLayout(sendCashPanel, BoxLayout.Y_AXIS));
		sendCashPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel("Send cash from:       "));
		tempPanel.add(new JLabel(
			"<html><span style=\"font-size:8px;\">" +
			"* Only addresses with a confirmed balance are shown as sources for sending!" +
		    "</span>  "));
		sendCashPanel.add(tempPanel);

		balanceAddressCombo = new JComboBox<>(new String[] { "" });
		comboBoxParentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		comboBoxParentPanel.add(balanceAddressCombo);
		sendCashPanel.add(comboBoxParentPanel);
		
		JLabel dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 3));
		sendCashPanel.add(dividerLabel);

		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel("Destination address:"));
		sendCashPanel.add(tempPanel);
		
		destinationAddressField = new JTextField(73);
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tempPanel.add(destinationAddressField);
		sendCashPanel.add(tempPanel);
				
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 3));
		sendCashPanel.add(dividerLabel);

		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel("Memo (optional):     "));
		tempPanel.add(new JLabel(
				"<html><span style=\"font-size:8px;\">" +
				"* Memo may be specified only if the destination is a Z (Private) address!" +
			    "</span>  "));
		sendCashPanel.add(tempPanel);
		
		destinationMemoField = new JTextField(73);
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tempPanel.add(destinationMemoField);
		sendCashPanel.add(tempPanel);		
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 3));
		sendCashPanel.add(dividerLabel);

		// Construct a more complex panel for the amount and transaction fee
		JPanel amountAndFeePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JPanel amountPanel = new JPanel(new BorderLayout());
		amountPanel.add(new JLabel("Amount to send:"), BorderLayout.NORTH);
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(destinationAmountField = new JTextField(13));
		destinationAmountField.setHorizontalAlignment(SwingConstants.RIGHT);
		tempPanel.add(new JLabel(" ZEC    "));
		amountPanel.add(tempPanel, BorderLayout.SOUTH);

        // Panel for xtn fee
		JPanel feePanel = new JPanel(new BorderLayout());
		feePanel.add(new JLabel("Miner Txn fee:"), BorderLayout.NORTH);
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(transactionFeeField = new JTextField(13));
		transactionFeeField.setText("0.0001"); // Default value
		transactionFeeField.setHorizontalAlignment(SwingConstants.RIGHT);		
		tempPanel.add(new JLabel(" ZEC"));
		feePanel.add(tempPanel, BorderLayout.SOUTH);

		// Donations make the world go round!!!!! And they keep David fed and happy, which makes the world go round
		JPanel donationPanel       = new JPanel(new BorderLayout());
		donationPanel.add(new JLabel("Wallet Dev Fee:"), BorderLayout.NORTH);
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(transactiondonationField = new JTextField(13));
		transactiondonationField.setText("0.0001"); // Default value
		transactiondonationField.setHorizontalAlignment(SwingConstants.RIGHT);
		tempPanel.add(new JLabel(" ZEC    "));
		donationPanel.add(tempPanel, BorderLayout.SOUTH);

		// add xtn fee to UI panel
		amountAndFeePanel.add(amountPanel);
		amountAndFeePanel.add(feePanel);
		amountAndFeePanel.add(donationPanel);
		sendCashPanel.add(amountAndFeePanel);		
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 3));
		sendCashPanel.add(dividerLabel);

		sendCashPanel.add(amountAndFeePanel);
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 3));
		sendCashPanel.add(dividerLabel);

        // Main sending UI
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendButton = new JButton("Send   \u27A4\u27A4\u27A4"));
		sendCashPanel.add(tempPanel);

		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 5));
		sendCashPanel.add(dividerLabel);
		
		JPanel warningPanel = new JPanel();
		warningPanel.setLayout(new BorderLayout(7, 3));
		JLabel warningL = new JLabel(
				"<html><span style=\"font-size:8px;\">" +
				" * When sending cash from a T (Transparent) address, the remining unspent balance is sent to another " +
				"auto-generated T address. When sending from a Z (Private) address, the remining unspent balance remains with " +
				"the Z address. In both cases the original sending address cannot be used for sending again until the " +
				"transaction is confirmed. The address is temporarily removed from the list! Freshly mined coins may only "+
				"be sent to a Z (Private) address." +
			    "</span>");
		warningPanel.add(warningL, BorderLayout.NORTH);
		sendCashPanel.add(warningPanel);
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 15));
		sendCashPanel.add(dividerLabel);
		
		// Build the operation status panel
		operationStatusPanel = new JPanel();
		sendCashPanel.add(operationStatusPanel);
		operationStatusPanel.setLayout(new BoxLayout(operationStatusPanel, BoxLayout.Y_AXIS));
		
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel("Last operation status: "));
        tempPanel.add(operationStatusLabel = new JLabel("N/A"));
        operationStatusPanel.add(tempPanel);		
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 6));
		operationStatusPanel.add(dividerLabel);

		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel("Progress: "));
        tempPanel.add(operationStatusProhgressBar = new JProgressBar(0, 200));
        operationStatusProhgressBar.setPreferredSize(new Dimension(250, 17));
        operationStatusPanel.add(tempPanel);		
        
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 13));
		operationStatusPanel.add(dividerLabel);
		
		// Wire the buttons
		sendButton.addActionListener(new ActionListener() 
		{	
			public void actionPerformed(ActionEvent e) 
			{
				try
			    {
					SendCashPanel.this.sendCash();
				} catch (Exception ex)
				{
					LOG.log(Level.WARNING, "", ex);
					
					String errMessage = "";
					if (ex instanceof WalletCallException)
					{
						errMessage = ((WalletCallException)ex).getMessage().replace(",", ",\n");
					}
					
					JOptionPane.showMessageDialog(
							SendCashPanel.this.getRootPane().getParent(), 
							"An unexpected error occurred when sending cash!\n" + 
							"Please ensure that the Zcash daemon is running and\n" +
							"parameters are correct. You may try again later...\n" +
							errMessage, 
							"Error in sending cash", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// Update the balances via timer and data gathering thread
		this.addressBalanceGatheringThread = new DataGatheringThread<String[][]>(
			new DataGatheringThread.DataGatherer<String[][]>() 
			{
				public String[][] gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					String[][] data = SendCashPanel.this.getAddressPositiveBalanceDataFromWallet();
					long end = System.currentTimeMillis();
					LOG.fine("Gathering of address/balance table data done in " + (end - start) + "ms." );
					
					return data;
				}
			}, 
			this.errorReporter, 10000, true);
		this.threads.add(addressBalanceGatheringThread);
		
		ActionListener alBalancesUpdater = new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					// TODO: if the user has opened the combo box - this closes it (maybe fix)
					SendCashPanel.this.updateWalletAddressPositiveBalanceComboBox();
				} catch (Exception ex)
				{
					LOG.log(Level.WARNING, "", ex);
					SendCashPanel.this.errorReporter.reportError(ex);
				}
			}
		};
		Timer timerBalancesUpdater = new Timer(15000, alBalancesUpdater);
		timerBalancesUpdater.setInitialDelay(3000);
		timerBalancesUpdater.start();
		this.timers.add(timerBalancesUpdater);
		
		// Add a popup menu to the destination address field - for convenience
		JMenuItem paste = new JMenuItem("Paste address");
		final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(paste);
        paste.addActionListener(new ActionListener() 
        {	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					String address = (String)Toolkit.getDefaultToolkit().getSystemClipboard().
							         getData(DataFlavor.stringFlavor);
					if ((address != null) && (address.trim().length() > 0))
					{
						SendCashPanel.this.destinationAddressField.setText(address);
					}
				} catch (Exception ex)
				{
					LOG.log(Level.WARNING, "", ex);
					// TODO: clipboard exception handling - do it better
					// java.awt.datatransfer.UnsupportedFlavorException: Unicode String
					//SendCashPanel.this.errorReporter.reportError(ex);
				}
			}
		});
        
        this.destinationAddressField.addMouseListener(new MouseAdapter()
        {
        	public void mousePressed(MouseEvent e)
        	{
                if ((!e.isConsumed()) && e.isPopupTrigger())
                {
                    popupMenu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
                    e.consume();
                };
        	}
        	
            public void mouseReleased(MouseEvent e)
            {
            	if ((!e.isConsumed()) && e.isPopupTrigger())
            	{
            		mousePressed(e);
            	}
            }
        });
		
	}
	
	
	private void sendCash()
		throws WalletCallException, IOException, InterruptedException
	{
		if (balanceAddressCombo.getItemCount() <= 0)
		{
			JOptionPane.showMessageDialog(
				SendCashPanel.this.getRootPane().getParent(), 
				"There are no addresses with a positive balance to send\n" +
				"cash from!", 
				"No funds available", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (this.balanceAddressCombo.getSelectedIndex() < 0)
		{
			JOptionPane.showMessageDialog(
				SendCashPanel.this.getRootPane().getParent(), 
				"Please select a source address with a current positive\n" +
				"balance to send cash from!", 
				"Please select source address", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		final String sourceAddress = this.lastAddressBalanceData[this.balanceAddressCombo.getSelectedIndex()][1];
		final String destinationAddress = this.destinationAddressField.getText();
		final String memo = this.destinationMemoField.getText();
		final String amount = this.destinationAmountField.getText();
		final String donation = this.transactiondonationField.getText();
		final String fee = this.transactionFeeField.getText();

		// Verify general correctness.
		String errorMessage = null;
		
		if ((sourceAddress == null) || (sourceAddress.trim().length() <= 20))
		{
			errorMessage = "Source address is invalid; it is too short or missing.";
		} else if (sourceAddress.length() > 512)
		{
			errorMessage = "Source address is invalid; it is too long.";
		}
		
		// TODO: full address validation
		if ((destinationAddress == null) || (destinationAddress.trim().length() <= 0))
		{
			errorMessage = "Destination address is invalid; it is missing.";
		} else if (destinationAddress.trim().length() <= 20)
		{
			errorMessage = "Destination address is invalid; it is too short.";
		} else if (destinationAddress.length() > 512)
		{
			errorMessage = "Destination address is invalid; it is too long.";
		}
		
		if ((amount == null) || (amount.trim().length() <= 0))
		{
			errorMessage = "Amount to send is invalid; it is missing.";
		} else 
		{
			try 
			{
				double d = Double.valueOf(amount);
			} catch (NumberFormatException nfe)
			{
				errorMessage = "Amount to send is invalid; it is not a number.";				
			}
		}
		
		if ((fee == null) || (fee.trim().length() <= 0))
		{
			errorMessage = "Transaction fee is invalid; it is missing.";
		} else 
		{
			try 
			{
				double d = Double.valueOf(fee);
			} catch (NumberFormatException nfe)
			{
				errorMessage = "Transaction fee is invalid; it is not a number.";				
			}
		}

		if ((donation == null) || (donation.trim().length() <= 0))
		{
		    errorMessage = "Wallet dev fee is missing or invalid.";
		} else 
		{
			try 
			{
			        double d = Double.valueOf(donation);
			} catch (NumberFormatException nfe)
			{
				errorMessage = "Wallet dev fee is invalid; it is not a number.";				
			}
		}


		if (errorMessage != null)
		{
			JOptionPane.showMessageDialog(
				SendCashPanel.this.getRootPane().getParent(), 
				errorMessage, "Sending parameters are incorrect", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// Nag the user if dev fee is zero!
		if (Double.valueOf(donation) <= 0 && nagged == 0) {
			JOptionPane.showMessageDialog(
				SendCashPanel.this.getRootPane().getParent(), 
				"Wallet dev fee is zero, and software is not free to make! Also cat food and kids' glasses are not free :-)", "You'll only see this nag screen once per session", JOptionPane.INFORMATION_MESSAGE);
			nagged = 1;
			return;
		    
		}
		    
		
		// Check for encrypted wallet
		final boolean bEncryptedWallet = this.clientCaller.isWalletEncrypted();
		if (bEncryptedWallet)
		{
			PasswordDialog pd = new PasswordDialog((JFrame)(SendCashPanel.this.getRootPane().getParent()));
			pd.setVisible(true);
			
			if (!pd.isOKPressed())
			{
				return;
			}
			
			this.clientCaller.unlockWallet(pd.getPassword());
		}
		
		// Call the wallet send method
		operationStatusID = this.clientCaller.sendCash(sourceAddress, destinationAddress, amount, memo, fee, donation);
				
		// Disable controls after send
		sendButton.setEnabled(false);
		balanceAddressCombo.setEnabled(false);
		destinationAddressField.setEnabled(false);
		destinationAmountField.setEnabled(false);
		destinationMemoField.setEnabled(false);
		transactionFeeField.setEnabled(false);
		transactiondonationField.setEnabled(false);
		// Start a timer to update the progress of the operation
		operationStatusCounter = 0;
		operationStatusTimer = new Timer(2000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					// TODO: Handle errors in case of restarted server while wallet is sending ...
					if (clientCaller.isSendingOperationComplete(operationStatusID))
					{
						if (clientCaller.isCompletedOperationSuccessful(operationStatusID))
						{
							operationStatusLabel.setText(
								"<html><span style=\"color:green;font-weight:bold\">SUCCESSFUL</span></html>");
							JOptionPane.showMessageDialog(
									SendCashPanel.this.getRootPane().getParent(), 
									"Successfully sent " + amount + " ZEC from address: \n" +
									sourceAddress + "\n" +
									"to address: \n" +
									destinationAddress + "\n", 
									"Cash sent successfully", JOptionPane.INFORMATION_MESSAGE);
						} else
						{
							String errorMessage = clientCaller.getOperationFinalErrorMessage(operationStatusID); 
							operationStatusLabel.setText(
								"<html><span style=\"color:red;font-weight:bold\">ERROR: " + errorMessage + "</span></html>");

							JOptionPane.showMessageDialog(
									SendCashPanel.this.getRootPane().getParent(), 
									"An error occurred when sending cash. Error message is:\n" +
									errorMessage + "\n\n" +
									"Please ensure that sending parameters are correct. You may try again later...\n", 
									"Error in sending cash", JOptionPane.ERROR_MESSAGE);

						}
						
						// Lock the wallet again 
						if (bEncryptedWallet)
						{
							SendCashPanel.this.clientCaller.lockWallet();
						}
						
						// Restore controls etc.
						operationStatusCounter = 0;
						operationStatusID      = null;
						operationStatusTimer.stop();
						operationStatusTimer = null;
						operationStatusProhgressBar.setValue(0);
						
						sendButton.setEnabled(true);
						balanceAddressCombo.setEnabled(true);
						destinationAddressField.setEnabled(true);
						destinationAmountField.setEnabled(true);
						transactiondonationField.setEnabled(true);
						transactionFeeField.setEnabled(true);
						destinationMemoField.setEnabled(true);
					} else
					{
						// Update the progress
						operationStatusLabel.setText(
							"<html><span style=\"color:orange;font-weight:bold\">IN PROGRESS</span></html>");
						operationStatusCounter += 2;
						int progress = 0;
						if (operationStatusCounter <= 100)
						{
							progress = operationStatusCounter;
						} else
						{
							progress = 100 + (((operationStatusCounter - 100) * 6) / 10);
						}
						operationStatusProhgressBar.setValue(progress);
					}
					
					SendCashPanel.this.repaint();
				} catch (Exception ex)
				{
					LOG.log(Level.WARNING, "", ex);
					SendCashPanel.this.errorReporter.reportError(ex);
				}
			}
		});
		operationStatusTimer.setInitialDelay(0);
		operationStatusTimer.start();
	}

	
	public void prepareForSending(String address) 
	{
	    destinationAddressField.setText(address);
	}
	
	
	private void updateWalletAddressPositiveBalanceComboBox()
		throws WalletCallException, IOException, InterruptedException
	{
		String[][] newAddressBalanceData = this.addressBalanceGatheringThread.getLastData();
		
		// The data may be null if nothing is yet obtained
		if (newAddressBalanceData == null)
		{
			return;
		}
		
		lastAddressBalanceData = newAddressBalanceData;
		
		comboBoxItems = new String[lastAddressBalanceData.length];
		for (int i = 0; i < lastAddressBalanceData.length; i++)
		{
			// Do numeric formatting or else we may get 1.1111E-5
			comboBoxItems[i] = 
				new DecimalFormat("########0.00######").format(Double.valueOf(lastAddressBalanceData[i][0]))  + 
				" - " + lastAddressBalanceData[i][1];
		}
		
		int selectedIndex = balanceAddressCombo.getSelectedIndex();
		boolean isEnabled = balanceAddressCombo.isEnabled();
		this.comboBoxParentPanel.remove(balanceAddressCombo);
		balanceAddressCombo = new JComboBox<>(comboBoxItems);
		comboBoxParentPanel.add(balanceAddressCombo);
		if ((balanceAddressCombo.getItemCount() > 0) &&
			(selectedIndex >= 0) &&
			(balanceAddressCombo.getItemCount() > selectedIndex))
		{
			balanceAddressCombo.setSelectedIndex(selectedIndex);
		}
		balanceAddressCombo.setEnabled(isEnabled);

		this.validate();
		this.repaint();
	}


	private String[][] getAddressPositiveBalanceDataFromWallet()
		throws WalletCallException, IOException, InterruptedException
	{
		// Z Addresses - they are OK
		String[] zAddresses = clientCaller.getWalletZAddresses();
		
		// T Addresses created inside wallet that may be empty
		String[] tAddresses = this.clientCaller.getWalletAllPublicAddresses();
		Set<String> tStoredAddressSet = new HashSet<>();
		for (String address : tAddresses)
		{
			tStoredAddressSet.add(address);
		}
		
		// T addresses with unspent outputs (even if not GUI created)...
		String[] tAddressesWithUnspentOuts = this.clientCaller.getWalletPublicAddressesWithUnspentOutputs();
		Set<String> tAddressSetWithUnspentOuts = new HashSet<>();
		for (String address : tAddressesWithUnspentOuts)
		{
			tAddressSetWithUnspentOuts.add(address);
		}
		
		// Combine all known T addresses
		Set<String> tAddressesCombined = new HashSet<>();
		tAddressesCombined.addAll(tStoredAddressSet);
		tAddressesCombined.addAll(tAddressSetWithUnspentOuts);
		
		String[][] tempAddressBalances = new String[zAddresses.length + tAddressesCombined.size()][];
		
		int count = 0;

		for (String address : tAddressesCombined)
		{
			String balance = this.clientCaller.getBalanceForAddress(address);
			if (Double.valueOf(balance) > 0)
			{
				tempAddressBalances[count++] = new String[] 
				{  
					balance, address
				};
			}
		}
		
		for (String address : zAddresses)
		{
			String balance = this.clientCaller.getBalanceForAddress(address);
			if (Double.valueOf(balance) > 0)
			{
				tempAddressBalances[count++] = new String[] 
				{  
					balance, address
				};
			}
		}

		String[][] addressBalances = new String[count][];
		System.arraycopy(tempAddressBalances, 0, addressBalances, 0, count);
		
		return addressBalances;
	}
	
	
}
