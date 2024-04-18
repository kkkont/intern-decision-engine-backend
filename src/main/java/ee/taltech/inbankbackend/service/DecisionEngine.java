package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();
    private int creditModifier = 0;

    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount   Requested loan amount
     * @param loanPeriod   Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException   If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException   If the requested loan period is invalid
     * @throws NoValidLoanException         If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
            verifyAge(personalCode);
        } catch (Exception | NotValidAgeException e) {
            return new Decision(null, null, e.getMessage());
        }

        creditModifier = getCreditModifier(personalCode);
        if (creditModifier == 0) throw new NoValidLoanException("No valid loan found! \nReason: debt");

        loanPeriod = adjustLoanPeriod(loanPeriod);
        int outputLoanAmount = calculateOutputLoanAmount(loanPeriod);

        double creditScore = getCreditScore(creditModifier, loanAmount, loanPeriod);
        if (creditScore < 1) {
            return new Decision(outputLoanAmount, loanPeriod, "Not approved due to bad credit score!");
        }

        return new Decision(outputLoanAmount, loanPeriod, null);

    }

    /**
     * Calculates highest valid amount and checks if it is smaller than maximum loan amount
     *
     * @return highest possible amount to loan
     */
    private int calculateOutputLoanAmount(int loanPeriod) {
        int highestValidAmount = highestValidLoanAmount(loanPeriod);
        return Math.min(DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT, highestValidAmount);
    }

    /**
     * If needed to adjust the loan period to get approved loan
     *
     * @return adjusted loan period
     */
    private int adjustLoanPeriod(int loanPeriod) {
        while (highestValidLoanAmount(loanPeriod) < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT && loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            loanPeriod++;
        }
        return loanPeriod;
    }


    /**
     * Calculates credit score based on credit modifier, loan amount and loan period
     *
     * @return credit score
     */
    private double getCreditScore(int creditModifier, Long loanAmount, int loanPeriod) {
        return ((double) creditModifier / loanAmount) * loanPeriod;
    }

    /**
     * Calculates the largest valid loan for the current credit modifier and loan period.
     *
     * @return Largest valid loan amount
     */
    private int highestValidLoanAmount(int loanPeriod) {

        return creditModifier * loanPeriod;
    }

    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code. Throws an error if customer has debt.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) throws NoValidLoanException {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        // 49002010965
        int constraint1 = 965;
        // 49002010976
        int constraint2 = 976;
        // 49002010987
        int constraint3 = 987;

        if (segment == constraint1) {
            return 0;
        } else if (segment == constraint2) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment == constraint3) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }
        // 49002010998 and others
        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount   Requested loan amount
     * @param loanPeriod   Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException   If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException   If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

    }

    /**
     * Verifies if the customer is in allowed age group
     *
     * @throws NoValidLoanException
     */
    private void verifyAge(String personalCode) throws NotValidAgeException {
        int minimumAge = 18;
        int maximumAge = 80 - DecisionEngineConstants.MAXIMUM_LOAN_PERIOD/12;

        int customerAge = findAge(personalCode);

        if (customerAge < minimumAge || customerAge > maximumAge) {
            throw new NotValidAgeException("Loan cannot be approved due to age constraints. Age: " + customerAge);
        }
    }

    /**
     * Finds customers age based on their personal code
     *
     * @return age
     */
    private int findAge(String personalCode) {
        int century = personalCode.charAt(0) == '5' || personalCode.charAt(0) == '6' ? 2000 : 1900;
        int birthYear = Integer.parseInt(personalCode.substring(1, 3)) + century;
        int birthMonth = Integer.parseInt(personalCode.substring(3, 5));
        int birthDay = Integer.parseInt(personalCode.substring(5, 7));

        LocalDate birthDate = LocalDate.of(birthYear, birthMonth, birthDay);
        LocalDate currentDate = LocalDate.now();

        Period age = Period.between(birthDate, currentDate);
        return age.getYears();
    }
}
