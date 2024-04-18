# TICKET-101

## Task
The task for TICKET-101 was to implement a decision engine for a loan approval system. The decision engine should evaluate personal codes, loan amounts, and loan periods to determine whether a loan should be approved, and if so, the maximum loan amount that can be approved. The system also needs to handle scenarios where a person has debt or falls under different segmentation categories, each with its own credit modifier. The solution should be designed with a focus on simplicity and adherence to SOLID principles.

## Code review

### Strength 
 - **Modularity:** The code is structured in a way that separates concerns, making it easier to understand and maintain. The calculateApprovedLoan method is responsible for orchestrating the decision-making process, while the validation and calculation logic are abstracted away.
 - **Error Handling:** The method includes comprehensive error handling, catching exceptions and returning a Decision object with an error message. This is a good practice for robustness and user-friendly error reporting.
 - **Validation:** There is a mechanism in place to validate the inputs, which is crucial for ensuring data integrity and preventing invalid operations.
 - **Documentation:** The code exhibits commendable documentation practices, making it both easily understandable and maintainable.

### Areas for improvement

 - **Integrating Credit Score Calculation:** Calculation should be based on the formula provided: (credit modifier / loan amount) * loan period. This calculation should be integrated into the decision-making process to determine whether a loan should be approved based on the calculated credit score.
 - **Missing hardcoded IDs:**  In real life the solution should connect to external registries and compose a comprehensive user profile, but for the sake of simplicity this part should be mocked as a hard coded result for certain personal codes.
 - **Unfinished If Statement and Potential Infinite Loop:** In the provided code snippet, there is a potential for an infinite loop due to the use of a while loop without a clear exit condition. Loop should be running until it reaches the maximum loan period. 
 - **Lack of SOLID Principles:**  The code does not fully adhere to SOLID principles, particularly the Single Responsibility Principle (SRP). The calculateApprovedLoan method is doing too much, including validation, decision logic, and error handling. Refactoring this method to delegate responsibilities to separate classes or methods would improve the code's maintainability and testability.

## Conclusion

The implementation of TICKET-101 showcases a solid foundation in designing a decision engine for loan approvals, with strengths in modularity, error handling, validation, and documentation. However, there are critical areas for improvement, particularly in integrating the credit score calculation, handling hardcoded IDs, avoiding potential infinite loops, and adhering to SOLID principles. Addressing these areas will enhance the system's robustness, maintainability, and testability, aligning it more closely with best practices in software development.