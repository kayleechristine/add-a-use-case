// UC-STU-deactivate-student's consequences for peer evaluation (BR-student-lifecycle): a deactivated teammate is not
// offered for evaluation (UC-EVA-submit-evaluation step 2), and the section report tags a deactivated student's row.
// The student and report calls are stubbed. Run against the dev stack with `npm run cy:open`.

const average = (studentId, firstName, lastName, deactivated) => ({
  studentId,
  week: '2023-W32',
  firstName,
  lastName,
  email: `${firstName[0].toLowerCase()}.${lastName.toLowerCase()}@abc.edu`,
  teamName: 'Team1',
  deactivated,
  averageTotalScore: 50,
  publicComments: [],
  privateComments: [],
  ratingAverages: []
})

const teammatesBody = {
  flag: true,
  code: 200,
  message: 'Find students by team id successfully',
  data: [
    {
      id: 4,
      username: 'j.smith@abc.edu',
      firstName: 'John',
      lastName: 'Smith',
      email: 'j.smith@abc.edu',
      enabled: true
    },
    {
      id: 5,
      username: 'e.hudson@abc.edu',
      firstName: 'Eric',
      lastName: 'Hudson',
      email: 'e.hudson@abc.edu',
      enabled: false
    },
    {
      id: 6,
      username: 'j.moon@abc.edu',
      firstName: 'Jerry',
      lastName: 'Moon',
      email: 'j.moon@abc.edu',
      enabled: true
    }
  ]
}

describe('Deactivated teammates', () => {
  it('leaves a deactivated teammate off the evaluation form (UC-EVA-submit-evaluation step 2)', () => {
    cy.login('j.smith@abc.edu', '123456')
    cy.intercept('GET', '**/students/teams/*', { body: teammatesBody }).as('teammates')
    cy.intercept('GET', '**/evaluations/evaluators/*/week/*', {
      body: { flag: true, code: 200, message: 'Find evaluations successfully', data: [] }
    }).as('ownEvaluations')

    cy.visit('/evaluations/submit-evaluations')
    cy.wait('@teammates')
    cy.wait('@ownEvaluations')

    cy.get('.el-table')
      .should('contain', 'John Smith')
      .and('contain', 'Jerry Moon')
      .and('not.contain', 'Eric Hudson')
  })

  it('opens a new submission when every stored evaluation is of a since-deactivated teammate (review of #3)', () => {
    cy.login('j.smith@abc.edu', '123456')
    cy.intercept('GET', '**/students/teams/*', { body: teammatesBody }).as('teammates')
    cy.intercept('GET', '**/evaluations/evaluators/*/week/*', {
      body: {
        flag: true,
        code: 200,
        message: 'Find evaluations successfully',
        data: [
          {
            evaluationId: 9,
            week: '2023-W32',
            evaluatorId: 4,
            evaluateeId: 5,
            evaluateeName: 'Eric Hudson',
            ratings: [],
            publicComment: '',
            privateComment: ''
          }
        ]
      }
    }).as('ownEvaluations')

    cy.visit('/evaluations/submit-evaluations')
    cy.wait('@teammates')
    cy.wait('@ownEvaluations')

    cy.get('.el-table')
      .should('contain', 'John Smith')
      .and('contain', 'Jerry Moon')
      .and('not.contain', 'Eric Hudson')
    cy.contains('You have NOT submitted the evaluation').should('be.visible')
  })

  it('tags a deactivated student in the section peer evaluation report', () => {
    cy.login('b.gates@abc.edu', '123456')
    cy.intercept('GET', '**/evaluations/sections/*/week/*', {
      body: {
        flag: true,
        code: 200,
        message: 'Generate peer evaluation report successfully',
        data: {
          sectionName: '2023-2024',
          week: '2023-W32',
          peerEvaluationAverages: [
            average(4, 'John', 'Smith', false),
            average(5, 'Eric', 'Hudson', true)
          ]
        }
      }
    }).as('report')

    cy.visit('/section-evaluations')
    cy.wait('@report')

    cy.contains('tr', 'Hudson').find('[data-cy=deactivated-tag]').should('contain', 'Deactivated')
    cy.contains('tr', 'Smith').find('[data-cy=deactivated-tag]').should('not.exist')
  })
})
