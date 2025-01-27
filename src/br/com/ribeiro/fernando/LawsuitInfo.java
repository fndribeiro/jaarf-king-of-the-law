package br.com.ribeiro.fernando;

public class LawsuitInfo {
	
	private final String date;
	private final String lawsuitNumber;
	private final String fullText;
	
	public LawsuitInfo(String date, String lawsuitNumber, String fullText) {
		this.date = date;
		this.lawsuitNumber = lawsuitNumber;
		this.fullText = fullText;
	}

	public String getDate() {
		return date;
	}

	public String getLawsuitNumber() {
		return lawsuitNumber;
	}

	public String getFullText() {
		return fullText;
	}
	
	
	

}
